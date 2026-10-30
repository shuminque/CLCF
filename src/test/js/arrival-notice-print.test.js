const assert = require('node:assert/strict');
const {test} = require('node:test');
const {renderNotice, init} = require('../../main/resources/static/js/arrival-notice-print.js');

const sample = {
    deliveryPoint: 'ZAB', tradeMode: '一般贸易', customer: '新(KS)', dimensions: '30.40',
    steelGrade: 'S2', steelMill: '江阴', furnaceNumber: 'E12605569XX', bundleCount: 32, weight: 30292
};

test('prints direct field names, combined specifications and sample quantities', () => {
    const html = renderNotice([sample, {...sample, customer: '新(HF)', furnaceNumber: 'E12604384XX', bundleCount: 5, weight: 4222}], '2026-07-28');
    assert.ok(html.includes('原材料到货通知 ZAB'));
    assert.ok(html.includes('2026年7月28日'));
    assert.ok(html.includes('26-7-28'));
    assert.ok(html.includes('<div class="notice-trade-mode">一般贸易</div><div class="notice-customer-name">新(KS)</div>'));
    assert.ok(html.includes('30.40S2'));
    assert.ok(html.includes('>32</td>'));
    assert.ok(html.includes('30,292'));
    assert.ok(html.includes('>5</td>'));
    assert.ok(html.includes('4,222'));
    assert.ok(html.includes('E12604384XX'));
    assert.ok(renderNotice([{...sample, furnaceNumber: 'H12608511XX/H12608520XX', bundleCount: 18, weight: 58214}], '2026-07-28')
        .includes('H12608511XX/H12608520XX'));
    assert.ok(html.includes('入库确认：'));
    assert.equal((html.match(/class="notice-page"/g) || []).length, 1);
    assert.equal((html.match(/class="notice-sheet"/g) || []).length, 5);
});

test('different delivery points receive separate notices and imported strings are escaped', () => {
    const html = renderNotice([sample, {...sample, deliveryPoint: 'SAB', customer: '<script>bad()</script>', furnaceNumber: '"<&'}], '2026-07-28');
    assert.equal((html.match(/class="notice-page"/g) || []).length, 2);
    assert.equal((html.match(/class="notice-sheet"/g) || []).length, 10);
    assert.ok(html.includes('原材料到货通知 SAB'));
    assert.ok(html.includes('&lt;script&gt;bad()&lt;/script&gt;'));
    assert.ok(html.includes('&quot;&lt;&amp;'));
    assert.ok(!html.includes('<script>'));
});

test('each A4 page repeats the same contents with five department labels, including two inspection copies', () => {
    const html = renderNotice([sample], '2026-07-28');
    const copies = html.split('<section class="notice-sheet">').slice(1);
    const labels = copies.map(copy => /class="notice-department" aria-label="([^"]+)"/.exec(copy)[1]);
    assert.deepEqual(labels, ['计划', '生管', '仓库', '受入检查', '受入检查']);
    const contents = copies.map(copy => copy.slice(0, copy.indexOf('<div class="notice-department"')));
    contents.forEach(content => assert.equal(content, contents[0]));
    assert.equal((html.match(/class="notice-empty-row"/g) || []).length, 10);
});

test('all materials remain on one five-copy A4 page without losing rows', () => {
    const rows = Array.from({length: 7}, (_, index) => ({...sample, customer: 'customer-' + (index + 1)}));
    const pages = renderNotice(rows, '2026-07-28').split('<section class="notice-page">').slice(1);
    assert.equal(pages.length, 1);
    assert.equal((pages[0].match(/class="notice-sheet"/g) || []).length, 5);
    rows.forEach((row, index) => {
        assert.equal(pages[0].split('>' + row.customer + '</div>').length - 1, 5);
        assert.equal(pages[0].split('<tr><td>' + (index + 1) + '</td>').length - 1, 5);
    });
    assert.equal(renderNotice([], '2026-07-28'), '');
});

test('blank fields render empty and fractional weights retain precision', () => {
    const html = renderNotice([{...sample, customer: null, steelGrade: null, weight: 0.125}], '2026-07-28');
    assert.ok(!html.includes('null'));
    assert.ok(html.includes('<div class="notice-trade-mode">一般贸易</div><div class="notice-customer-name"></div>'));
    assert.ok(html.includes('>30.40</td>'));
    assert.ok(html.includes('0.125'));
});

test('latest date wins; empty and failed responses cannot print stale materials', async () => {
    const events = {};
    const fields = Object.fromEntries(['noticeArrivalDate', 'noticePreview', 'noticeStatus', 'noticePrintButton', 'noticeForm', 'noticeExportButton', 'noticeExportStatus'].map(id =>
        [id, {value: '', innerHTML: '', textContent: '', querySelectorAll() { return []; },
            addEventListener(type, handler) { events[id + ':' + type] = handler; }}]));
    const requests = [];
    let printCount = 0;
    global.document = {getElementById: id => fields[id]};
    global.window = {location: {search: '?arrivalDate=2026-07-28'}, print() { printCount++; },
        addEventListener(type, handler) { events['window:' + type] = handler; }};
    global.fetch = url => new Promise((resolve, reject) => requests.push({url, resolve, reject}));
    try {
        init();
        assert.equal(fields.noticeArrivalDate.value, '2026-07-28');
        assert.equal(fields.noticePrintButton.disabled, true);
        fields.noticeArrivalDate.value = '2026-07-29';
        const current = events['noticeArrivalDate:change']();
        requests[1].resolve({ok: true, json: async () => [sample]});
        await current;
        assert.ok(fields.noticePreview.innerHTML.includes('26-7-29'));
        events['noticePrintButton:click']();
        assert.equal(printCount, 1);
        requests[0].resolve({ok: true, json: async () => []});
        await new Promise(resolve => setImmediate(resolve));
        assert.equal(fields.noticePrintButton.disabled, false);
        fields.noticeArrivalDate.value = '2026-07-30';
        const empty = events['noticeArrivalDate:change']();
        assert.equal(fields.noticePreview.innerHTML, '');
        events['noticePrintButton:click']();
        assert.equal(printCount, 1);
        requests[2].resolve({ok: true, json: async () => []});
        await empty;
        assert.equal(fields.noticePrintButton.disabled, true);
        assert.ok(fields.noticeStatus.textContent.includes('没有材料'));
        const failed = events['noticeArrivalDate:change']();
        requests[3].reject(new Error('network unavailable'));
        await failed;
        assert.ok(fields.noticeStatus.textContent.includes('查询失败'));
        assert.equal(fields.noticePrintButton.disabled, true);
    } finally {
        delete global.document;
        delete global.window;
        delete global.fetch;
    }
});
