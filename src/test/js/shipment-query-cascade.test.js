const assert = require('node:assert/strict');
const {test} = require('node:test');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/shipment-query-cascade.js'), 'utf8');

function setup() {
    function select(items) {
        return {value: '', disabled: false, options: items.map(value => ({value})),
            add(option) { this.options.push(option); }};
    }
    const fields = {
        steelMill: select(['', 'A', 'B']),
        steelGrade: select(['', 'G1', 'G2', 'G3']),
        dimensions: select(['', 'S1', 'S2', 'S3'])
    };
    const events = {};
    const requests = [];
    const messages = [];
    const context = {
        document: {getElementById: () => ({querySelector: selector => fields[selector.match(/name="(.*?)"/)[1]]})},
        Option: function(text, value) { this.text = text; this.value = value; }
    };
    vm.createContext(context);
    vm.runInContext(source, context);
    const api = context.initShipmentQueryCascade({
        on: (event, handler) => { events[event] = handler; },
        render: (type, filter) => { assert.equal(type, 'select'); assert.equal(filter, 'shipmentSearchForm'); }
    }, {ajax: request => requests.push(request)}, {msg: message => messages.push(message)});
    return {fields, requests, messages, api,
        mill(value) { fields.steelMill.value = value; events['select(querySteelMill)'](); },
        grade(value) { fields.steelGrade.value = value; events['select(querySteelGrade)'](); }};
}

const options = select => Array.from(select.options, option => option.value);
const combinations = [
    {steelGrade: 'G1', dimensions: 'S1'},
    {steelGrade: 'G1', dimensions: 'S1'},
    {steelGrade: 'G2', dimensions: 'S2'},
    {steelGrade: '', dimensions: ''},
    {steelGrade: null, dimensions: null}
];

test('steel mill filters both descendants; grade further filters sizes and clears stale selection', () => {
    const s = setup();
    s.fields.steelGrade.value = 'G3';
    s.fields.dimensions.value = 'S3';
    s.mill('A');
    assert.equal(s.requests[0].url, '/shipment/material-options');
    assert.equal(s.requests[0].data.steelMill, 'A');
    assert.equal(s.api.ready(), false);
    assert.equal(s.fields.steelGrade.value, '');
    assert.equal(s.fields.dimensions.value, '');
    assert.equal(s.fields.steelGrade.disabled, true);
    s.requests[0].success(combinations);
    assert.deepEqual(options(s.fields.steelGrade), ['', 'G1', 'G2']);
    assert.deepEqual(options(s.fields.dimensions), ['', 'S1', 'S2']);
    assert.equal(s.api.ready(), true);
    s.fields.dimensions.value = 'S2';
    s.grade('G1');
    assert.deepEqual(options(s.fields.dimensions), ['', 'S1']);
    assert.equal(s.fields.dimensions.value, '');
    s.grade('');
    assert.deepEqual(options(s.fields.dimensions), ['', 'S1', 'S2']);
});

test('latest mill wins even if responses arrive in reverse order', () => {
    const s = setup();
    s.mill('A');
    s.mill('B');
    s.requests[1].success([{steelGrade: 'G3', dimensions: 'S3'}]);
    s.requests[0].success(combinations);
    s.requests[0].error();
    assert.deepEqual(options(s.fields.steelGrade), ['', 'G3']);
    assert.deepEqual(options(s.fields.dimensions), ['', 'S3']);
    assert.equal(s.messages.length, 0);
});

test('clearing mill restores original lists and ignores pending responses', () => {
    const s = setup();
    s.mill('A');
    s.mill('');
    s.requests[0].success(combinations);
    assert.deepEqual(options(s.fields.steelGrade), ['', 'G1', 'G2', 'G3']);
    assert.deepEqual(options(s.fields.dimensions), ['', 'S1', 'S2', 'S3']);
    assert.equal(s.fields.steelGrade.disabled, false);
    assert.equal(s.api.ready(), true);
});

test('empty results disable descendants but still allow searching the mill', () => {
    const s = setup();
    s.mill('A');
    s.requests[0].success([]);
    assert.deepEqual(options(s.fields.steelGrade), ['']);
    assert.deepEqual(options(s.fields.dimensions), ['']);
    assert.equal(s.fields.steelGrade.disabled, true);
    assert.equal(s.fields.dimensions.disabled, true);
    assert.equal(s.api.ready(), true);
});

test('failed or malformed responses keep search blocked until retry succeeds', () => {
    const s = setup();
    s.mill('A');
    s.requests[0].error();
    assert.equal(s.api.ready(), false);
    assert.equal(s.messages.length, 1);
    s.mill('B');
    s.requests[1].success({error: 'login required'});
    assert.equal(s.api.ready(), false);
    s.mill('A');
    s.requests[2].success(combinations);
    assert.equal(s.api.ready(), true);
});
