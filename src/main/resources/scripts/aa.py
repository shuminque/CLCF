import matplotlib.pyplot as plt
import matplotlib.font_manager as fm
import pandas as pd
from sqlalchemy import create_engine
import os
from datetime import datetime

# 数据库连接配置
database_url = 'mysql+mysqlconnector://root:123456@localhost:3308/clck'
engine = create_engine(database_url)

# 获取当前日期
current_date = datetime.now().strftime('%Y-%m-%d')

# 查询语句
query = f"""
WITH in_out_count AS (
    SELECT
        unique_identifier,
        placement_area AS area,
        SUM(CASE WHEN operation_type IN ('入库', '返库') THEN 1 ELSE 0 END) AS in_count,
        SUM(CASE WHEN operation_type = '出库' THEN 1 ELSE 0 END) AS out_count
    FROM
        shipment_details
    WHERE
        time <= '{current_date}'
        AND placement_area REGEXP '^(通道区[0-9]+|[A-F][0-9]*区|E[A-B][0-9]+区)$'
    GROUP BY
        unique_identifier, placement_area
    HAVING
        in_count > out_count
),
latest_in_info AS (
    SELECT
        sd.*,
        io.in_count,
        io.out_count
    FROM
        shipment_details sd
    JOIN in_out_count io ON sd.unique_identifier = io.unique_identifier
    WHERE
        (operation_type = '入库' OR operation_type = '转库' OR operation_type = '返库')
        AND time <= '{current_date}'
        AND placement_area REGEXP '^(通道区[0-9]+|[A-F][0-9]*区|E[A-B][0-9]+区)$'
)
SELECT DISTINCT
    latest_in_info.placement_area AS area,  
    latest_in_info.dimensions,
    latest_in_info.steel_mill,
    latest_in_info.steel_grade,
    GROUP_CONCAT(DISTINCT latest_in_info.furnace_number ORDER BY latest_in_info.furnace_number ASC SEPARATOR '\n') AS furnace_numbers
FROM
    latest_in_info
GROUP BY
    latest_in_info.placement_area,
    latest_in_info.dimensions,
    latest_in_info.steel_mill,
    latest_in_info.steel_grade
ORDER BY
    LEFT(latest_in_info.placement_area, 1),  -- 按字母排序 A-F
    CAST(SUBSTRING(latest_in_info.placement_area, 2, LENGTH(latest_in_info.placement_area) - 2) AS UNSIGNED),  -- 按数字排序
    latest_in_info.steel_grade,
    latest_in_info.dimensions;
"""

# 执行查询并将结果存储在数据框中
df = pd.read_sql(query, engine)

# 设置全局字体为simsun.ttc中的宋体
plt.rcParams['font.sans-serif'] = ['SimSun']  # 选择宋体字体
plt.rcParams['axes.unicode_minus'] = False  # 解决负号显示为方块的问题

# 设置保存路径和时间格式
save_dir = os.path.join(os.path.expanduser("~"), "Desktop", "看板图")
date_str = datetime.now().strftime('%Y%m%d')  # 当前日期，格式为 YYYYMMDD
daily_dir = os.path.join(save_dir, date_str)  # 对应日期的文件夹

# 创建日期文件夹（如果不存在）
os.makedirs(daily_dir, exist_ok=True)

# 按区划分数据
df['区'] = df['area'].str[0]  # 提取区的第一个字母（如A区，B区等）
zones = df['区'].unique()  # 获取所有区

# 创建FontProperties对象用于设置字体加粗
font_prop = fm.FontProperties(fname='C:\\Windows\\Fonts\\simsun.ttc', size=15, weight='bold')

# 将炉号列中的内容进行换行处理
def wrap_text(text, width=30):
    return '\n'.join([text[i:i+width] for i in range(0, len(text), width)])

# 为每个区生成单独的图表
for zone in zones:
    df_zone = df[df['区'] == zone]  # 过滤出当前区的数据

    # 合并相同的 `area` 列
    merged_data = []
    last_area = None
    for _, row in df_zone.iterrows():
        if row['area'] == last_area:
            row_data = [''] + row[1:-1].tolist()  # 去掉最后一列，并将区域列替换为空字符串
        else:
            row_data = row[:-1].tolist()  # 只去掉最后一列，保留区域列
            last_area = row['area']
        merged_data.append(row_data)

    # 将数据拆分为多列，每列最多12行
    max_rows_per_column = 12
    split_data = [merged_data[i:i + max_rows_per_column] for i in range(0, len(merged_data), max_rows_per_column)]

    # 如果最后一列不满12行，用空行补足
    for column_data in split_data:
        while len(column_data) < max_rows_per_column:
            column_data.append([''] * 5)  # 追加空行

    # 创建多列数据的表格
    num_columns = len(split_data)
    fig, ax = plt.subplots(figsize=(25, 12))  # 设置图表尺寸

    # 隐藏x轴和y轴
    ax.xaxis.set_visible(False)
    ax.yaxis.set_visible(False)
    ax.set_frame_on(False)

    # 为每列生成表格
    for col_idx, column_data in enumerate(split_data):
        table = ax.table(cellText=column_data,
                         colLabels=['区域', '尺寸', '钢厂', '钢种', '炉号'],  # 中文标题
                         cellLoc='center',
                         loc='center',
                         bbox=[col_idx * (1/num_columns), 0, 1/num_columns, 1])  # 每列位置和宽度

        # 调整表格样式
        table.auto_set_font_size(False)
        table.set_fontsize(15)  # 可以根据需要调整字体大小

        # 设置表格字体加粗
        for key, cell in table.get_celld().items():
            cell.set_text_props(fontproperties=font_prop)
            cell.PAD = 0.01  # 减少单元格内容的内边距

        # 将“区域”列标灰
        for i in range(len(column_data)):
            cell = table[(i, 0)]  # 选中第1列（索引从0开始）
            cell.set_facecolor('#D3D3D3')  # 设置灰色背景

    # 设置图表标题
    plt.title(f'{zone}区数据', fontsize=20, fontweight='bold')

    # 调整布局，减少边距
    plt.tight_layout(pad=3)

       # 保存图表到文件
    file_name = f"{zone}区-{date_str}.png"
    save_path = os.path.join(daily_dir, file_name)
    plt.savefig(save_path, bbox_inches='tight')
