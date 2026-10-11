// 仅联动记录查询表单，编辑弹窗仍使用原有基础数据。
function initShipmentQueryCascade(form, $, layer) {
    var searchForm = document.getElementById('searchForm');
    var mill = searchForm.querySelector('[name="steelMill"]');
    var grade = searchForm.querySelector('[name="steelGrade"]');
    var dimensions = searchForm.querySelector('[name="dimensions"]');
    var allGrades = values(grade.options);
    var allDimensions = values(dimensions.options);
    var combinations = [];
    var requestVersion = 0;
    var ready = true;

    function values(options) {
        return Array.from(options).map(function(option) { return option.value; }).filter(Boolean);
    }

    function uniqueValues(rows, field) {
        return Array.from(new Set(rows.map(function(row) { return row[field]; }).filter(function(value) {
            return typeof value === 'string' && value.trim() !== '';
        })));
    }

    function setOptions(select, items, placeholder, disabled) {
        select.options.length = 0;
        select.add(new Option(placeholder, ''));
        items.forEach(function(value) { select.add(new Option(value, value)); });
        select.value = '';
        select.disabled = disabled;
    }

    function render() {
        form.render('select', 'shipmentSearchForm');
    }

    function updateDimensions() {
        var rows = combinations.filter(function(row) {
            return !grade.value || row.steelGrade === grade.value;
        });
        var items = mill.value ? uniqueValues(rows, 'dimensions') : allDimensions;
        setOptions(dimensions, items, items.length ? '选择尺寸' : '暂无匹配尺寸', !items.length);
    }

    function updateMill() {
        var version = ++requestVersion;
        combinations = [];
        if (!mill.value) {
            ready = true;
            setOptions(grade, allGrades, '选择钢材等级', false);
            setOptions(dimensions, allDimensions, '选择尺寸', false);
            render();
            return;
        }
        ready = false;
        setOptions(grade, [], '正在加载等级', true);
        setOptions(dimensions, [], '正在加载尺寸', true);
        render();
        $.ajax({
            url: '/shipment/material-options',
            type: 'GET',
            dataType: 'json',
            timeout: 15000,
            data: {steelMill: mill.value},
            success: function(rows) {
                // 快速切换钢厂时，较早请求的响应不能覆盖当前选项。
                if (version !== requestVersion) return;
                if (!Array.isArray(rows)) {
                    fail();
                    return;
                }
                combinations = rows;
                var grades = uniqueValues(rows, 'steelGrade');
                setOptions(grade, grades, grades.length ? '选择钢材等级' : '暂无匹配等级', !grades.length);
                updateDimensions();
                ready = true;
                render();
            },
            error: fail
        });

        function fail() {
            if (version !== requestVersion) return;
            setOptions(grade, [], '加载失败，请重选钢厂', true);
            setOptions(dimensions, [], '加载失败，请重选钢厂', true);
            render();
            layer.msg('无法加载该钢厂的等级和尺寸，请重新选择钢厂重试');
        }
    }

    form.on('select(querySteelMill)', updateMill);
    form.on('select(querySteelGrade)', function() {
        updateDimensions();
        render();
    });
    if (mill.value) updateMill();
    return {ready: function() { return ready; }};
}
