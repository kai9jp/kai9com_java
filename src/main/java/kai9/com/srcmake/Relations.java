package kai9.com.srcmake;

import lombok.AllArgsConstructor;

//Java,ReactMakerで利用
//特殊制御【relation】用
@AllArgsConstructor
public class Relations {
    public String tableA;
    public String columnA;
    public String tableB;
    public String columnB;
    public String src_column;

    // Java,ReactMakerで利用
    // オブジェクトの等価性を比較するための equals メソッドのオーバーライド(contains 実行時に内部的にequalsが使われる)
    @Override
    public boolean equals(Object o) {
        // 同一のオブジェクトである場合、true を返します
        if (this == o) return true;
        // 渡されたオブジェクトが null であるか、クラスが異なる場合、false を返します
        if (o == null || getClass() != o.getClass()) return false;
        // キャストして比較対象の 'relations' オブジェクトを取得
        Relations relations = (Relations) o;
        // 各フィールドを比較し、全てが等しい場合に true を返す
        return tableA.equals(relations.tableA) &&
                columnA.equals(relations.columnA) &&
                tableB.equals(relations.tableB) &&
                columnB.equals(relations.columnB) &&
                src_column.equals(relations.src_column);
    }
}
