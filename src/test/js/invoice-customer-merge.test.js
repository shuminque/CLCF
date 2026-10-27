const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const {test} = require('node:test');

const html = fs.readFileSync(path.join(__dirname,
    '../../main/resources/templates/clck/pcpage/invoicing-management.html'), 'utf8');
const script = html.match(/<script>\s*([\s\S]*?)<\/script>/)[1];
const bundle = fs.readFileSync(path.join(__dirname,
    '../../main/resources/static/lib/layui-v2.8.17/layui.js'), 'utf8');
const escape = vm.runInNewContext('(' + bundle.match(/escape:(function\(t\)\{[\s\S]*?}),unescape:/)[1] + ')');
const sample = {arrival_date: '2026-10-23', customer: 'Customer <A>', steel_mill: 'Mill',
    steel_grade: 'Grade', dimensions: '35.40', trade_mode: 'Trade', weight: 30,
    unit_price: 6.50863, invoice_application: '', merge_customers: false, customer_filter: ''};

function setup() {
    const events = {};
    const requests = [];
    const reloads = [];
    const hiddenColumns = [];
    let options, dialog, selected = [], printed = '';
    const $ = {ajax(request) {
        if (request.url === '/taxs/') request.success([]);
        else requests.push(request);
    }};
    vm.runInNewContext(script, {$, console: {log() {}},
        document: {getElementsByClassName() { return []; }},
        window: {open() { return {document: {write(value) { printed += value; }, close() {}}, print() {}}; }},
        layer: {open(config) { dialog = config; }, close() {}, msg() {}},
        layui: {use(modules, callback) { callback(); },
            table: {
                render(config) {
                    options = config;
                    return {reload(config = {}, deep) {
                        reloads.push({config: structuredClone(config), deep});
                    }};
                },
                on(event, callback) { events[event] = callback; },
                hideCol(id, column) { hiddenColumns.push({...column}); },
                checkStatus() { return {data: selected}; }
            },
            form: {on(event, callback) { events[event] = callback; }, render() {}},
            laydate: {render() {}}, util: {escape}
        }
    });
    return {events, requests, reloads, hiddenColumns, options,
        search(fields = {}) {
            events['submit(data-search-btn)']({field: {date: '', customer: '', steelMill: '',
                steelGrade: '', dimensions: '', invoiceApplication: '', ...fields}});
        },
        toggle(checked) { events['checkbox(mergeCustomers)']({elem: {checked}}); },
        detail(row) { events['tool(recordsTableFilter)']({event: 'detail', data: row}); },
        edit(row) {
            events['tool(recordsTableFilter)']({event: 'edit', data: row});
            dialog.yes(1, {find(selector) {
                return {val() { return selector === '#newUnitPrice' ? '7.12345' : 'paid'; }};
            }});
        },
        print(rows) {
            selected = rows;
            events['toolbar(recordsTableFilter)']({event: 'printSelected'});
            return printed;
        }
    };
}

test('merge checkbox is beside selected print and defaults to separate customers', () => {
    const page = setup();
    assert.equal(page.options.toolbar, '#invoiceToolbar');
    assert.equal(page.options.where.mergeCustomers, false);
    assert.match(html, /lay-event="printSelected"[\s\S]*?lay-filter="mergeCustomers"/);
    assert.equal(page.options.initSort, undefined);
    page.options.done({data: []});
});

test('checkbox toggles regrouping while preserving submitted filters; searching keeps the mode', () => {
    const page = setup();
    page.search({customer: 'Customer A', steelMill: 'Mill', date: '2026-10-01 - 2026-10-31'});
    page.toggle(true);
    assert.equal(page.reloads.at(-1).config.where.mergeCustomers, true);
    assert.equal(page.reloads.at(-1).config.where.customer, 'Customer A');
    assert.equal(page.reloads.at(-1).config.where.startDate, '2026-10-01');
    assert.equal(page.reloads.at(-1).config.where.endDate, '2026-10-31');
    assert.deepEqual(page.hiddenColumns.at(-1), {field: 'customer', hide: true});
    page.search({steelMill: 'Other Mill'});
    assert.equal(page.reloads.at(-1).config.where.mergeCustomers, true);
    assert.equal(page.reloads.at(-1).config.where.customer, '');
    assert.equal(page.reloads.at(-1).config.where.startDate, undefined);
    assert.equal(page.reloads.at(-1).deep, false);
    page.toggle(false);
    assert.equal(page.reloads.at(-1).config.where.mergeCustomers, false);
    assert.equal(page.reloads.at(-1).config.where.steelMill, 'Other Mill');
    assert.deepEqual(page.hiddenColumns.at(-1), {field: 'customer', hide: false});
});

test('separate and merged printouts use their data mode and keep equal totals', () => {
    const separate = setup().print([sample, {...sample, customer: 'Customer B', weight: 40}]);
    assert.ok(separate.includes('Customer &lt;A&gt;'));
    assert.ok(separate.includes('Customer B'));
    assert.ok(separate.includes('colspan="6"'));
    const page = setup();
    page.toggle(false);
    const merged = page.print([{...sample, weight: 70, merge_customers: true}]);
    assert.ok(!merged.includes('Customer'));
    assert.ok(merged.includes('colspan="5"'));
    const amount = (70 * sample.unit_price).toFixed(2);
    assert.ok(separate.includes('>' + amount + '</td>'));
    assert.ok(merged.includes('>' + amount + '</td>'));
    assert.ok(separate.includes('>70</td>'));
    assert.ok(merged.includes('>70</td>'));
});

test('merged detail and edit requests use the original customer filter and settlement group', () => {
    const page = setup();
    const row = {...sample, customer: 'Multiple customers', merge_customers: true,
        customer_filter: 'Customer A', invoice_application: 'unpaid'};
    page.detail(row);
    const detail = page.requests.at(-1);
    assert.equal(detail.data.mergeCustomers, true);
    assert.equal(detail.data.customer, 'Customer A');
    assert.equal(detail.data.original_invoice_application, 'unpaid');
    page.edit(row);
    const edit = page.requests.at(-1);
    assert.equal(edit.url, '/shipment/updateInvoice');
    assert.equal(edit.data.customer, 'Customer A');
    assert.equal(edit.data.original_invoice_application, 'unpaid');
    edit.success({success: true});
    assert.equal(page.reloads.length, 1);
});

test('merged null status is omitted and separate operations retain the row customer', () => {
    const page = setup();
    page.detail({...sample, merge_customers: true, invoice_application: null});
    assert.equal(page.requests.at(-1).data.customer, '');
    assert.equal('original_invoice_application' in page.requests.at(-1).data, false);
    page.edit({...sample, merge_customers: true, invoice_application: null});
    assert.equal('original_invoice_application' in page.requests.at(-1).data, false);
    page.toggle(true);
    page.detail(sample);
    assert.equal(page.requests.at(-1).data.mergeCustomers, false);
    assert.equal(page.requests.at(-1).data.customer, sample.customer);
});
