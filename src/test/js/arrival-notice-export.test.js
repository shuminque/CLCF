const assert = require('node:assert/strict');
const {test} = require('node:test');
const {init} = require('../../main/resources/static/js/arrival-notice-print.js');

const excelType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

async function setup(t) {
    const previous = {document: global.document, window: global.window, fetch: global.fetch};
    const events = {};
    const fields = Object.fromEntries(['noticeArrivalDate', 'noticePreview', 'noticeStatus',
        'noticePrintButton', 'noticeForm', 'noticeExportButton', 'noticeExportStatus'].map(id => [id, {
        value: '', textContent: '', innerHTML: '', disabled: false, valid: true,
        checkValidity() { return this.valid; }, reportValidity() { this.reported = true; },
        querySelectorAll() { return []; },
        addEventListener(type, handler) { events[id + ':' + type] = handler; }
    }]));
    const requests = [];
    const downloads = [];
    const revoked = [];
    global.document = {
        getElementById: id => fields[id],
        body: {appendChild() {}},
        createElement() {
            return {click() { downloads.push({href: this.href, filename: this.download}); }, remove() {}};
        }
    };
    global.window = {
        location: {search: '?arrivalDate=2026-10-25'}, addEventListener() {},
        URL: {createObjectURL: () => 'blob:monthly-export', revokeObjectURL: url => revoked.push(url)},
        setTimeout: handler => handler()
    };
    global.fetch = url => url.includes('/export?')
        ? new Promise((resolve, reject) => requests.push({url, resolve, reject}))
        : Promise.resolve({ok: true, json: async () => []});
    t.after(() => Object.assign(global, previous));
    init();
    await new Promise(resolve => setImmediate(resolve));
    return {fields, requests, downloads, revoked, exportMonth: () => events['noticeExportButton:click']()};
}

test('exports selected month even when the selected day is empty; later date edits do not rename the download', async t => {
    const {fields, requests, downloads, revoked, exportMonth} = await setup(t);
    assert.equal(fields.noticePrintButton.disabled, true);
    const pending = exportMonth();
    assert.equal(requests[0].url, '/shipment/arrival-notice/export?arrivalDate=2026-10-25');
    assert.equal(fields.noticeExportButton.disabled, true);
    await exportMonth();
    assert.equal(requests.length, 1);
    fields.noticeArrivalDate.value = '2026-11-15';
    requests[0].resolve({ok: true, status: 200, headers: new Headers({'Content-Type': excelType}),
        blob: async () => new Blob(['excel'], {type: excelType})});
    await pending;
    assert.deepEqual(downloads, [{href: 'blob:monthly-export', filename: '原材料到货通知_2026-10_明细.xlsx'}]);
    assert.deepEqual(revoked, ['blob:monthly-export']);
    assert.equal(fields.noticeExportButton.disabled, false);
    assert.equal(fields.noticeExportButton.textContent, '导出当月明细');
    assert.ok(fields.noticeExportStatus.textContent.includes('2026年10月'));
    assert.ok(fields.noticeStatus.textContent.includes('没有材料'));
});

test('empty months show a message without downloading a file', async t => {
    const {fields, requests, downloads, exportMonth} = await setup(t);
    const pending = exportMonth();
    requests[0].resolve({ok: true, status: 204});
    await pending;
    assert.equal(downloads.length, 0);
    assert.equal(fields.noticeExportStatus.textContent, '2026年10月没有到货明细可导出。');
    assert.equal(fields.noticeExportButton.disabled, false);
});

test('missing and invalid dates do not start an export', async t => {
    const {fields, requests, exportMonth} = await setup(t);
    fields.noticeArrivalDate.value = '';
    await exportMonth();
    assert.equal(requests.length, 0);
    assert.ok(fields.noticeArrivalDate.reported);
    fields.noticeArrivalDate.value = '2026-02-30';
    fields.noticeArrivalDate.valid = false;
    await exportMonth();
    assert.equal(requests.length, 0);
    assert.ok(fields.noticeExportStatus.textContent.includes('有效的到货日'));
});

test('network failures, HTTP errors and login pages show failure and allow retry', async t => {
    const {fields, requests, downloads, exportMonth} = await setup(t);
    const responses = [
        new Error('network unavailable'),
        {ok: false, status: 500},
        {ok: true, status: 200, headers: new Headers({'Content-Type': 'text/html'})}
    ];
    for (const [index, response] of responses.entries()) {
        const pending = exportMonth();
        if (response instanceof Error) requests[index].reject(response);
        else requests[index].resolve(response);
        await pending;
        assert.equal(fields.noticeExportButton.disabled, false);
        assert.ok(fields.noticeExportStatus.textContent.includes('导出失败'));
    }
    assert.equal(downloads.length, 0);
});
