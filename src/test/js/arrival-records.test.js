const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const {test} = require('node:test');

const templates = path.join(__dirname, '../../main/resources/templates/clck/pcpage');
const html = fs.readFileSync(path.join(templates, 'arrivalRecords.html'), 'utf8');
const script = html.match(/<script>\s*([\s\S]*?)<\/script>/)[1];

function setup(rows = []) {
    const events = {}, reloads = [], exports = [], messages = [];
    const cells = {tradeMode: {innerText: ''}, weight: {innerText: ''}};
    let options, exportClick;
    function $(selector) {
        if (selector === '#operationType') throw new Error('Removed search control must not be accessed');
        return {
            length: 0, click() {}, append() {},
            val() { return selector === '#date' ? '2026-10-01 - 2026-10-31' : ''; },
            on(event, callback) {
                if (selector === '#customExportButton' && event === 'click') exportClick = callback;
            }
        };
    }
    vm.runInNewContext(script, {
        $, console,
        document: {
            addEventListener() {},
            getElementsByClassName() {
                return [{querySelector(selector) {
                    const field = selector.match(/data-field="([^"]+)"/)[1];
                    return {getElementsByClassName() { return [cells[field]]; }};
                }}];
            }
        },
        initShipmentQueryCascade: () => ({ready: () => true}),
        XLSX: {
            utils: {
                json_to_sheet(data) { return {data: JSON.parse(JSON.stringify(data))}; },
                sheet_add_aoa() {}, book_new() { return {}; },
                book_append_sheet(book, sheet) { book.sheet = sheet; }
            },
            writeFile(book, name) { exports.push({name, rows: book.sheet.data}); }
        },
        layui: {
            use(modules, callback) { callback(); },
            layer: {msg(message) { messages.push(message); }},
            laydate: {render() {}},
            form: {on(event, callback) { events[event] = callback; }},
            table: {
                cache: {recordTable: rows},
                render(config) {
                    options = config;
                    return {reload(config) { reloads.push(JSON.parse(JSON.stringify(config))); }};
                },
                on(event, callback) { events[event] = callback; }
            }
        }
    });
    return {
        options, reloads, cells, exports, messages,
        search(fields = {}) {
            const field = Object.fromEntries(['uniqueIdentifier', 'steelMill', 'steelGrade', 'tradeMode',
                'dimensions', 'purchaser', 'customer', 'furnaceNumber', 'supplierBatchNumber',
                'placementArea', 'deliveryPoint', 'state', 'time', 'time2'].map(key => [key, '']));
            Object.assign(field, fields);
            events['submit(data-search-btn)']({field});
        },
        showAndExport() {
            options.done({data: rows});
            exportClick();
        }
    };
}

test('initial and subsequent arrival searches use the dedicated endpoint; flow page keeps its endpoint', () => {
    const page = setup();
    assert.equal(page.options.url, '/shipment/arrivals');
    page.search({time: '2026-10-19 - 2026-10-19', customer: '客户甲', steelMill: '江阴材'});
    const search = page.reloads.at(-1);
    assert.equal(search.url, '/shipment/arrivals');
    assert.deepEqual(search.where, {startDate: '2026-10-19', endDate: '2026-10-19',
        customer: '客户甲', steelMill: '江阴材'});
    assert.equal(search.page.curr, 1);
    page.search();
    assert.deepEqual(page.reloads.at(-1).where, {});
    const records = fs.readFileSync(path.join(templates, 'shipmentRecords.html'), 'utf8');
    assert.match(records, /url: '\/shipment'/);
    assert.ok(!records.includes('/shipment/arrivals'));
    assert.ok(!page.options.initSort);
});

test('table totals and category export use the same original arrival rows including unoperated rows', () => {
    const base = {tradeMode: '一般贸易', customer: '客户甲', steelMill: '江阴材', steelGrade: '3B', dimensions: '32.35'};
    const page = setup([
        {...base, id: 1, uniqueIdentifier: 'M1', operationType: '入库', weight: 1000},
        {...base, id: 2, uniqueIdentifier: 'M2', operationType: null, weight: 2000},
        {...base, id: 3, uniqueIdentifier: 'M3', operationType: '', weight: 3000}
    ]);
    page.showAndExport();
    assert.equal(page.cells.tradeMode.innerText, '3');
    assert.equal(page.cells.weight.innerText, '6000.00');
    assert.equal(page.exports.length, 1);
    assert.equal(page.exports[0].rows.reduce((sum, row) => sum + row.weight, 0), 6000);
    assert.equal(page.exports[0].name, '2026_10_01_2026_10_31_汇总.xlsx');
});

test('empty arrival results show zero totals and do not attempt an invalid export', () => {
    const page = setup();
    page.showAndExport();
    assert.equal(page.cells.tradeMode.innerText, '0');
    assert.equal(page.cells.weight.innerText, '0.00');
    assert.equal(page.exports.length, 0);
    assert.deepEqual(page.messages, ['没有可导出的到货数据']);
});
