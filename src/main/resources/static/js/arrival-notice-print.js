(function () {
    'use strict';

    function text(value) {
        return value == null ? '' : String(value);
    }

    function escapeHtml(value) {
        return text(value).replace(/[&<>"']/g, function (character) {
            return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[character];
        });
    }

    function dateParts(value) {
        return /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
    }

    function shortDate(value) {
        var parts = dateParts(value);
        return parts ? parts[1].slice(2) + '-' + Number(parts[2]) + '-' + Number(parts[3]) : value;
    }

    function longDate(value) {
        var parts = dateParts(value);
        return parts ? parts[1] + '年' + Number(parts[2]) + '月' + Number(parts[3]) + '日' : value;
    }

    function formatWeight(value) {
        return Number(value).toLocaleString('en-US', {maximumFractionDigits: 6});
    }

    function customerHtml(row) {
        var tradeMode = text(row.tradeMode);
        var customer = text(row.customer);
        return '<div class="notice-customer">' +
            (tradeMode ? '<div class="notice-trade-mode">' + escapeHtml(tradeMode) + '</div>' : '') +
            '<div class="notice-customer-name">' + escapeHtml(customer) + '</div></div>';
    }

    function renderNotice(rows, arrivalDate) {
        var groups = new Map();
        rows.forEach(function (row) {
            var point = text(row.deliveryPoint);
            if (!groups.has(point)) groups.set(point, []);
            groups.get(point).push(row);
        });
        var pages = [];
        var departments = ['计划', '生管', '仓库', '受入检查', '受入检查'];
        groups.forEach(function (items, point) {
            var body = items.map(function (row, index) {
                return '<tr><td>' + (index + 1) + '</td><td>' + escapeHtml(shortDate(arrivalDate)) +
                    '</td><td>' + customerHtml(row) +
                    '</td><td>' + escapeHtml(text(row.dimensions) + text(row.steelGrade)) +
                    '</td><td>' + escapeHtml(row.bundleCount) + '</td><td class="notice-weight">' +
                    escapeHtml(formatWeight(row.weight)) + '</td><td>' + escapeHtml(row.steelMill) +
                    '</td><td>' + escapeHtml(row.furnaceNumber) + '</td></tr>';
            }).join('');
            for (var empty = items.length; empty < 3; empty++) {
                body += '<tr class="notice-empty-row" aria-hidden="true">' +
                    '<td></td>'.repeat(8) + '</tr>';
            }
            var copies = departments.map(function (department) {
                return '<section class="notice-sheet"><div class="notice-copy-content"><h1>原材料到货通知' +
                    (point ? ' ' + escapeHtml(point) : '') + '</h1>' +
                    '<p class="notice-document-code">QS/ZAB 4-06-06 (A/0)</p><p class="notice-date">' +
                    escapeHtml(longDate(arrivalDate)) + '</p><div class="notice-table-area"><table class="notice-table">' +
                    '<colgroup><col style="width:4%"><col style="width:9%"><col style="width:18%">' +
                    '<col style="width:18%"><col style="width:6%"><col style="width:10%">' +
                    '<col style="width:14%"><col style="width:21%"></colgroup>' +
                    '<thead><tr><th>No.</th><th>到货日</th><th>顾客名称</th><th>规格</th>' +
                    '<th>捆数</th><th>KGS/PCS</th><th>钢厂</th><th>备注</th></tr></thead>' +
                    '<tbody>' + body + '</tbody></table>' +
                    '<div class="notice-department" aria-label="' + department + '">' +
                    department.split('').map(function (character) { return '<span>' + character + '</span>'; }).join('') + '</div>' +
                    '</div><div class="notice-footer">' +
                    '<span>制单：</span><span>入库确认：</span></div></div></section>';
            }).join('');
            pages.push('<section class="notice-page">' + copies + '</section>');
        });
        return pages.join('');
    }

    function fitNoticePages(preview) {
        preview.querySelectorAll('.notice-sheet').forEach(function (sheet) {
            var content = sheet.querySelector('.notice-copy-content');
            content.style.transform = '';
            var style = window.getComputedStyle(sheet);
            var availableHeight = sheet.clientHeight - parseFloat(style.paddingTop) - parseFloat(style.paddingBottom);
            var scale = Math.min(1, availableHeight / content.getBoundingClientRect().height);
            content.style.transform = 'scale(' + scale + ')';
        });
    }

    function init() {
        var dateInput = document.getElementById('noticeArrivalDate');
        var preview = document.getElementById('noticePreview');
        var status = document.getElementById('noticeStatus');
        var printButton = document.getElementById('noticePrintButton');
        var exportButton = document.getElementById('noticeExportButton');
        var exportStatus = document.getElementById('noticeExportStatus');
        var requestVersion = 0;
        var loadedDate = '';
        var initialDate = new URLSearchParams(window.location.search).get('arrivalDate');
        var now = new Date();
        dateInput.value = initialDate && dateParts(initialDate) ? initialDate :
            now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' +
            String(now.getDate()).padStart(2, '0');

        function loadNotice() {
            var version = ++requestVersion;
            var date = dateInput.value;
            loadedDate = '';
            preview.innerHTML = '';
            printButton.disabled = true;
            if (!date) {
                status.textContent = '请选择到货日。';
                return Promise.resolve();
            }
            status.textContent = '正在查询到货材料…';
            return fetch('/shipment/arrival-notice?arrivalDate=' + encodeURIComponent(date))
                .then(function (response) {
                    if (!response.ok) throw new Error('查询失败');
                    return response.json();
                })
                .then(function (rows) {
                    if (version !== requestVersion) return;
                    if (!Array.isArray(rows)) throw new Error('返回数据格式异常');
                    if (!rows.length) {
                        status.textContent = '所选到货日没有材料数据。';
                        return;
                    }
                    preview.innerHTML = renderNotice(rows, date);
                    fitNoticePages(preview);
                    if (document.fonts) {
                        document.fonts.ready.then(function () {
                            if (version === requestVersion) fitNoticePages(preview);
                        });
                    }
                    loadedDate = date;
                    printButton.disabled = false;
                    status.textContent = '已合并为 ' + rows.length + ' 项，共 ' +
                        rows.reduce(function (total, row) { return total + Number(row.bundleCount); }, 0) + ' 捆。';
                })
                .catch(function () {
                    if (version !== requestVersion) return;
                    status.textContent = '查询失败，请确认登录状态并重试。';
                });
        }

        document.getElementById('noticeForm').addEventListener('submit', function (event) {
            event.preventDefault();
            loadNotice();
        });
        dateInput.addEventListener('change', loadNotice);
        exportButton.addEventListener('click', function () {
            if (exportButton.disabled) return Promise.resolve();
            var date = dateInput.value;
            if (!dateParts(date) || !dateInput.checkValidity()) {
                exportStatus.textContent = '请先选择有效的到货日，导出该日期所在月份的全部明细。';
                dateInput.reportValidity();
                return Promise.resolve();
            }
            var month = date.slice(0, 7);
            var monthLabel = date.slice(0, 4) + '年' + Number(date.slice(5, 7)) + '月';
            exportButton.disabled = true;
            exportButton.textContent = '正在导出…';
            exportStatus.textContent = '正在导出' + monthLabel + '的全部到货明细…';
            return fetch('/shipment/arrival-notice/export?arrivalDate=' + encodeURIComponent(date))
                .then(function (response) {
                    if (response.status === 204) {
                        exportStatus.textContent = monthLabel + '没有到货明细可导出。';
                        return null;
                    }
                    if (!response.ok || (response.headers.get('Content-Type') || '').split(';')[0] !==
                            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet') {
                        throw new Error('导出失败');
                    }
                    return response.blob();
                })
                .then(function (blob) {
                    if (!blob) return;
                    var url = window.URL.createObjectURL(blob);
                    var link = document.createElement('a');
                    link.href = url;
                    link.download = '原材料到货通知_' + month + '_明细.xlsx';
                    document.body.appendChild(link);
                    link.click();
                    link.remove();
                    window.setTimeout(function () { window.URL.revokeObjectURL(url); }, 1000);
                    exportStatus.textContent = monthLabel + '的全部到货明细已导出。';
                })
                .catch(function () {
                    exportStatus.textContent = '导出失败，请确认登录状态并重试。';
                })
                .then(function () {
                    exportButton.disabled = false;
                    exportButton.textContent = '导出当月明细';
                });
        });
        printButton.addEventListener('click', function () {
            if (!printButton.disabled && loadedDate === dateInput.value) {
                fitNoticePages(preview);
                window.print();
            }
        });
        window.addEventListener('beforeprint', function () { fitNoticePages(preview); });
        loadNotice();
    }

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = {renderNotice: renderNotice, init: init};
    } else {
        init();
    }
})();
