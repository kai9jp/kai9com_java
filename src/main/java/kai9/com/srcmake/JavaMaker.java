package kai9.com.srcmake;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Sheet;

import kai9.com.common.Kai9comUtils;
import kai9.libs.Kai9Util;
import kai9.libs.Kai9Utils;
import kai9.libs.PoiUtil;

public class JavaMaker {
    private static String RN = "\r\n";

    public static void Make(Sheet pWs, String OurDir, String Svn_Path, boolean isTargetStrLeave, String packageName1,
            String packageName2) throws IOException {

        // テーブル定義書のフォーマットに関する各エラーチェックは、React側で実施するので、こちらでは行わない。

        // "論理名称(和名
        int lRow1 = PoiUtil.findRow(pWs, "#R1#");
        int lCol_TablleName_J = PoiUtil.findCol(pWs, lRow1, "テーブル名(和名)") + 2;
        String lTable_Name_J = PoiUtil.GetCellValue(pWs, lRow1, lCol_TablleName_J);

        // "テーブル名
        int lRow2 = PoiUtil.findRow(pWs, "#R2#");
        int lCol_TablleName = PoiUtil.findCol(pWs, lRow2, "テーブル名") + 2;
        String lTable_Name = PoiUtil.GetCellValue(pWs, lRow2, lCol_TablleName).toLowerCase();

        // カラム番号を特定
        int lTitleRow = PoiUtil.findRow(pWs, "#R4#");
        int lCol_No = PoiUtil.findCol(pWs, lTitleRow, "No");
        int lCol_FieldName = PoiUtil.findCol(pWs, lTitleRow, "カラム名");
        int lCol_DataType = PoiUtil.findCol(pWs, lTitleRow, "データ型");
        int lCol_IsPK = PoiUtil.findCol(pWs, lTitleRow, "PK");

        boolean is_bytea_exist = false;// BLOBの存在確認用
        boolean is_date_exist = false;// 日付型の存在確認用
        boolean is_generated_exist = false;// 自動採番型の存在確認用
        short id_count = 0;// 主キー数
        int lTopRow = PoiUtil.findRow(pWs, "#R5#");
        for (int Rownum = lTopRow; Rownum < pWs.getLastRowNum(); Rownum++) {
            if (PoiUtil.GetStringValue(pWs, Rownum, lCol_No).equals("")) {
                continue;
            }
            String data_type = PoiUtil.GetStringValue(pWs, Rownum, lCol_DataType).toLowerCase();
            // BLOBの存在確認
            if (data_type.equals("bytea"))
                is_bytea_exist = true;
            // 日付型の存在確認
            if (data_type.equals("timestamp") || data_type.equals("date") || data_type.equals("time")) {
                is_date_exist = true;
            }
            // 自動採番型の存在確認
            if (data_type.equals("smallserial") || data_type.equals("serial") || data_type.equals("bigserial")) {
                is_generated_exist = true;
            }
            // 主キー数をカウント
            String ColumnName = PoiUtil.GetStringValue(pWs, Rownum, lCol_FieldName).toLowerCase();
            if (PoiUtil.GetStringValue(pWs, Rownum, lCol_IsPK).contentEquals("〇")
                    || PoiUtil.GetStringValue(pWs, Rownum, lCol_IsPK).contentEquals("○")) {
                if (!ColumnName.equals("modify_count")) {// modify_countにはIDを付けない
                    id_count++;
                }
            }
        }

        // modelクラス作成
        // クラス名
        String classname;
        if (lTable_Name.substring(lTable_Name.length() - 2, lTable_Name.length()).equals("_a")) {
            // クラス名では、テーブル名の「_a」をカットする
            classname = lTable_Name.substring(0, lTable_Name.length() - 2);
        } else {
            classname = lTable_Name;
        }
        // クラス名_原本
        String src_classname = "single_table";// 全て小文字

        // ======================================================================
        // モデル作成
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\model");
        // 雛形から複製
        Path sourcePath = Paths.get(Svn_Path + "\\main\\java\\kai9\\tmpl\\model\\" + src_classname + ".java");
        Path targetPath = Paths
                .get(OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\model\\" + classname + ".java");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        File file = new File(
                OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\model\\" + classname + ".java");
        String content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);

        if (!isTargetStrLeave) {
            // import(日付型)
            String targetStr = "//【制御:型】日付";
            content = manipulateString(content, targetStr, is_date_exist);
            // import(自動採番キー)
            targetStr = "//【制御:型】自動採番①";
            content = manipulateString(content, targetStr, is_generated_exist);
            targetStr = "//【制御:型】自動採番②";
            content = manipulateString(content, targetStr, is_generated_exist);
            // relation
            targetStr = "//【制御:型】relation";
            content = manipulateString(content, targetStr, !Kai9ComUtils.RelationsList.isEmpty());
        }
        // import(複合キー)
        String replaceStr = "";
        if (id_count >= 2) {
            replaceStr += "import java.io.Serializable;" + RN;
            replaceStr += "import javax.persistence.IdClass;" + RN;
            replaceStr += "import javax.persistence.Embeddable;" + RN;
            replaceStr += "import javax.persistence.Embedded;" + RN;
        }
        if (isTargetStrLeave) {
            content = content.replace("//【制御:型】複合キー" + RN, "//【制御:型】複合キー" + RN + replaceStr);
        } else {
            content = content.replace("//【制御:型】複合キー" + RN, replaceStr);
        }

        // クラス(複合キー)-----------------------------------------------------
        replaceStr = "";
        if (id_count >= 2) {
            replaceStr += "/**" + RN;
            replaceStr += " *複合キー用のクラス(" + lTable_Name_J + ")" + RN;
            replaceStr += " */" + RN;
            replaceStr += "@Embeddable" + RN;
            replaceStr += "@Data" + RN;
            replaceStr += "@SuppressWarnings(\"serial\")" + RN;
            replaceStr += "class " + classname + "_key implements Serializable{" + RN;
            replaceStr += "	@Embedded" + RN;
            for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
                Models Models = Kai9ComUtils.modelList.get(i);
                if (!Models.is_pk)
                    continue;
                if (Models.columnname.equals("modify_count"))
                    continue;// modify_countは無視
                replaceStr += "	private " + CnvType(Models.data_type) + " " + Models.columnname + ";" + RN;
            }
            replaceStr += "}" + RN;
        }
        if (isTargetStrLeave) {
            content = content.replace("//【制御:クラス】複合キー" + RN, "//【制御:クラス】複合キー" + RN + replaceStr);
        } else {
            content = content.replace("//【制御:クラス】複合キー" + RN, replaceStr);
        }

        // import(複合キー)
        replaceStr = "";
        if (id_count >= 2) {
            replaceStr += "@IdClass(" + classname + "_key.class)" + RN;
        }
        if (isTargetStrLeave) {
            content = content.replace("//【制御:アノテーション】複合キー" + RN, "//【制御:アノテーション】複合キー" + RN + replaceStr);
        } else {
            content = content.replace("//【制御:アノテーション】複合キー" + RN, replaceStr);
        }
        // 各カラム
        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);

            // コメント(フィールド名)
            replaceStr += "    /**" + RN;
            replaceStr += "     * " + Models.FieldName_J + RN;
            replaceStr += "     */" + RN;

            // 主キー
            if (Models.is_pk) {
                if (!Models.columnname.equals("modify_count")) {// modify_countにはIDを付けない
                    replaceStr += "    @Id" + RN;
                }
            }
            // 自動採番
            if (Models.data_type.equals("smallserial") || Models.data_type.equals("serial")
                    || Models.data_type.equals("bigserial")) {
                replaceStr += "    @GeneratedValue(strategy = GenerationType.IDENTITY)" + RN;
            }

            replaceStr += "    @Column(name = \"" + Models.columnname + "\")" + RN;
            replaceStr += "    private " + CnvType(Models.data_type) + " " + Models.columnname + ";" + RN;
            replaceStr += "" + RN;
        }
        // 末尾の改行をカット
        if (replaceStr.endsWith("\r\n")) {
            replaceStr = replaceStr.substring(0, replaceStr.length() - 2);
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】カラム", "【制御:終了】カラム",
                isTargetStrLeave);

        // relation
        // パターンの定義
        List<Map.Entry<String, String>> replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            // テーブル名の末尾から「_a」又は「_b」を取り除く
            String table_name = relation.tableA;
            if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                table_name = table_name.substring(0, table_name.length() - 2);
            }
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", relation.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", relation.columnB));
        }
        String startMarker = "【制御:開始】relation";
        String endMarker = "【制御:終了】relation";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // 置換
        content = content.replace(src_classname, classname);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // リクエスト作成
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\dto");
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\main\\java\\kai9\\tmpl\\dto\\" + src_classname + "_Request.java");
        targetPath = Paths.get(
                OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\dto\\" + classname + "_Request.java");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(
                OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\dto\\" + classname + "_Request.java");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);

        if (!isTargetStrLeave) {
            // import(日付型)
            String targetStr = "//【制御:型】日付";
            content = manipulateString(content, targetStr, is_date_exist);
            // import(BLOB)
            replaceStr = "";
            if (is_bytea_exist) {
                replaceStr += "import com.fasterxml.jackson.annotation.JsonProperty;" + RN;
            }
            content = content.replace("//【制御:型】BLOB" + RN, replaceStr);
        }

        // 各カラム
        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);

            // コメント(フィールド名)
            replaceStr += "    /**" + RN;
            replaceStr += "     * " + Models.FieldName_J + RN;
            replaceStr += "     */" + RN;

            // BLOB
            if (Models.data_type.equals("bytea")) {
                replaceStr += "    @JsonProperty(\"" + Models.columnname
                        + "_dumy\")//API連携時のエラー回避策。別名にする事でnullが入る。別途MultipartFileで受け取る。BLOB型はFILE前程で自動生成する。" + RN;
            }

            replaceStr += "    private " + CnvType(Models.data_type) + " " + Models.columnname + ";" + RN;
            replaceStr += "" + RN;
        }
        // 末尾の改行をカット
        if (replaceStr.endsWith("\r\n")) {
            replaceStr = replaceStr.substring(0, replaceStr.length() - 2);
        }
        // 「[{/* [制御:開始]対象カラム② */}」から始まり、「//[制御:終了]対象カラム②」で終わる箇所と、生成したコードを置換する
//        pattern = Pattern.compile("^\\s*//【制御:開始】カラム.*?//【制御:終了】カラム\\s*$", Pattern.DOTALL | Pattern.MULTILINE);
//        matcher = pattern.matcher(content);
//        content = matcher.replaceAll(RN+replaceStr);
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】カラム", "【制御:終了】カラム",
                isTargetStrLeave);

        // relation
        // パターンの定義
        replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            // テーブル名の末尾から「_a」又は「_b」を取り除く
            String table_name = relation.tableA;
            if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                table_name = table_name.substring(0, table_name.length() - 2);
            }
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", relation.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", relation.columnB));
            for (Models model : Kai9ComUtils.modelList) {
                if (model.columnname.equals(relation.columnA)) {
                    replacements.add(new AbstractMap.SimpleEntry<>("関連ID", model.FieldName_J));
                }
                if (model.columnname.equals(relation.columnB)) {
                    replacements.add(new AbstractMap.SimpleEntry<>("関連データ", model.FieldName_J));
                }
            }
        }
        startMarker = "【制御:開始】relation";
        endMarker = "【制御:終了】relation";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // 置換
        content = content.replace(src_classname, classname);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // リポジトリ作成
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\repository");
        // 雛形から複製
        sourcePath = Paths
                .get(Svn_Path + "\\main\\java\\kai9\\tmpl\\repository\\" + src_classname + "_Repository.java");
        targetPath = Paths.get(OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\repository\\" + classname
                + "_Repository.java");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\repository\\" + classname
                + "_Repository.java");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        // 置換
        content = content.replace(src_classname, classname);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // サービス作成
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\service");
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\main\\java\\kai9\\tmpl\\service\\" + src_classname + "_Service.java");
        targetPath = Paths.get(
                OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\service\\" + classname + "_Service.java");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(
                OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\service\\" + classname + "_Service.java");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        if (!isTargetStrLeave) {
            // import(日付型)
            String targetStr = "//【制御:型】日付";
            content = manipulateString(content, targetStr, is_date_exist);
        }

        // 全検索--------------------------------------------------------------
        // プライマリキー記憶
        String IDs = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.columnname.equals("modify_count")) {
                continue;
            } // modify_countは無視
            if (!Models.is_pk) {
                continue;
            }
            if (IDs.equals("")) {
                IDs = IDs + Models.columnname;
            } else {
                IDs = IDs + "," + Models.columnname;
            }
        }
        String targetStr = "String sql = \"select * from " + src_classname + "_a order by s_pk\";";
        replaceStr = "String sql = \"select * from " + classname + "_a order by " + IDs + "\";";
        content = content.replace(targetStr, replaceStr);

        // 新規登録--------------------------------------------------------------
        replaceStr = "";
        String IDs1 = "";
        String IDs2 = "";
        String indent = "            ";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            String ColumnName = Models.columnname.substring(0, 1).toUpperCase() + Models.columnname.substring(1);// 1文字目を大文字変換
            // modify_count制御
            if (Models.columnname.equals("modify_count")) {
                if (Models.columnname.equals("modify_count")) {
                    replaceStr += indent + classname + ".setModify_count(1);// 新規登録は1固定" + RN;
                }
            } else if (Models.columnname.contains("update_date")) {
                replaceStr += indent + classname
                        + ".setUpdate_date(new java.sql.Timestamp(new java.util.Date().getTime()));" + RN;
                // 一般カラム(modify_count以外)
            } else {
                if (!Models.data_type.contains("serial") // 自動採番型は無視
                        & !ColumnName.equals("Update_u_id"))// 更新ユーザは無視
                {
                    if (Models.data_type.equals("bytea")) {
                        replaceStr += indent + "if (" + classname + "_request.get" + ColumnName + "() != null) "
                                + classname + ".set" + ColumnName + "(" + classname + "_request.get" + ColumnName
                                + "());" + RN;
                    } else {
                        replaceStr += indent + classname + ".set" + ColumnName + "(" + classname + "_request.get"
                                + ColumnName + "());" + RN;
                    }
                }
            }
            // PK制御
            if (!Models.is_pk)
                continue;
            // modify_countは無視
            if (Models.columnname.equals("modify_count"))
                continue;
            if (IDs1.equals("")) {
                IDs1 = Models.columnname + " = ?";
                IDs2 = classname + ".get" + ColumnName + "()";
            } else {
                IDs1 = IDs1 + " and " + Models.columnname + " = ?";
                IDs2 = IDs2 + "," + classname + ".get" + ColumnName + "()";
            }
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】カラム:新規登録", "【制御:終了】カラム:新規登録",
                isTargetStrLeave);

        //// 履歴の登録:SQL実行
        targetStr = "String sql = \"insert into " + src_classname + "_b select * from " + src_classname
                + "_a where s_pk = ?\";";
        replaceStr = "String sql = \"insert into " + classname + "_b select * from " + classname + "_a where " + IDs1
                + "\";";
        content = content.replace(targetStr, replaceStr);
        targetStr = "jdbcTemplate.update(sql, " + src_classname + ".getS_pk());";
        replaceStr = "jdbcTemplate.update(sql, " + IDs2 + ");";
        content = content.replace(targetStr, replaceStr);

        // 更新--------------------------------------------------------------
        // PK制御
        String IDs3 = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (!Models.is_pk) {
                continue;
            }
            if (Models.columnname.equals("modify_count")) {
                continue;
            } // modify_countを除く
            String ColumnName = Models.columnname.substring(0, 1).toUpperCase() + Models.columnname.substring(1);// 1文字目を大文字変換
            if (IDs3.equals("")) {
                IDs3 = classname + "UpdateRequest.get" + ColumnName + "()";
            } else {
                IDs3 = IDs3 + "," + classname + "UpdateRequest.get" + ColumnName + "()";
            }

        }
        // findById
        targetStr = "findById(" + src_classname + "UpdateRequest.getS_pk());";
        replaceStr = "findById(" + IDs3 + ");";
        content = content.replace(targetStr, replaceStr);

        // 変更判定
        boolean isBigDecimalExisi = false;
        replaceStr = "";
        indent = "            ";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            String ColumnName = Models.columnname.substring(0, 1).toUpperCase() + Models.columnname.substring(1);// 1文字目を大文字変換
            if (Models.columnname.contains("update_date")) {
                // update_dateは無視
                continue;
            } 
            if (Models.columnname.equals("modify_count")) {
                // modify_countは無視
                continue;
            } 
            if (Models.columnname.contains("update_u_id")) {
                // update_u_idは無視
                continue;
            }
            else if (Models.data_type.contains("numeric")) {
                replaceStr += indent + "if (isBigDecimalChanged(" + classname + ".get" + ColumnName + "(), " + classname + "UpdateRequest.get" + ColumnName + "())) IsChange = true;" + RN;
                isBigDecimalExisi = true;
                continue;
            }
            replaceStr += indent + "if (!Objects.equals(" + classname + ".get" + ColumnName + "(), " + classname + "UpdateRequest.get" + ColumnName + "())) IsChange = true;" + RN;
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】カラム:変更判定", "【制御:終了】カラム:変更判定",
                isTargetStrLeave);

        // 更新処理
        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            String ColumnName = Models.columnname.substring(0, 1).toUpperCase() + Models.columnname.substring(1);// 1文字目を大文字変換
            if (Models.columnname.equals("modify_count")) {
                // modify_count制御(親子関係がある場合、親と子で制御方法を変える)
                if (Models.columnname.equals("modify_count")) {
                    replaceStr += indent + classname + ".set" + ColumnName + "(" + classname + "UpdateRequest.get"
                            + ColumnName + "() + 1);// 更新回数+1" + RN;
                }
            } else if (Models.columnname.contains("update_date")) {
                // update_dateは無視
                replaceStr += indent + classname + ".set" + ColumnName
                        + "(new java.sql.Timestamp(new java.util.Date().getTime()));" + RN;
            } else {
                // 一般カラム(modify_count以外)
                if (Models.is_pk) {
                    continue;
                } // PKは無視
                if (ColumnName.equals("Update_u_id")) {
                    continue;
                } // 更新ユーザは無視
                if (Models.data_type.equals("bytea")) {
                    replaceStr += indent + "if (" + classname + "UpdateRequest.get" + ColumnName + "() != null) "
                            + classname + ".set" + ColumnName + "(" + classname + "UpdateRequest.get" + ColumnName
                            + "());" + RN;
                } else {
                    replaceStr += indent + classname + ".set" + ColumnName + "(" + classname + "UpdateRequest.get"
                            + ColumnName + "());" + RN;
                }
            }
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】カラム:更新", "【制御:終了】カラム:更新",isTargetStrLeave);

        //BigDecimal用比較関数
        if (!isBigDecimalExisi) {
            replacements = new ArrayList<>();
            startMarker = "【制御:開始】isBigDecimalChanged";
            endMarker = "【制御:終了】isBigDecimalChanged";
            content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);
            
        }

        // 主キー検索--------------------------------------------------------------
        // PK制御
        String ID4s = "";
        String ID5s = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (!Models.is_pk) {
                continue;
            } // pk以外は無視
            if (Models.columnname.equals("modify_count")) {
                continue;
            } // modify_countは無視
            if (ID4s.equals("")) {
                ID4s = CnvType(Models.data_type) + " " + Models.columnname;
                ID5s = Models.columnname;
            } else {
                ID4s = ID4s + ", " + CnvType(Models.data_type) + " " + Models.columnname;
                ID5s = ID5s + ", " + Models.columnname;
            }
        }
        targetStr = "public " + src_classname + " findById(Integer s_pk)";
        replaceStr = "public " + classname + " findById(" + ID4s + ")";
        content = content.replace(targetStr, replaceStr);

        targetStr = "String sql = \"select * from " + src_classname + "_a where s_pk = ?\"";
        replaceStr = "String sql = \"select * from " + classname + "_a where " + IDs1 + "\"";
        content = content.replace(targetStr, replaceStr);

        targetStr = "jdbcTemplate.queryForObject(sql, rowMapper, s_pk)";
        replaceStr = "jdbcTemplate.queryForObject(sql, rowMapper, " + ID5s + ")";
        content = content.replace(targetStr, replaceStr);

        // 物理削除--------------------------------------------------------------
        targetStr = "public void delete(Integer s_pk)";
        replaceStr = "public void delete(" + ID4s + ")";
        content = content.replace(targetStr, replaceStr);

        // 置換
        content = content.replace(src_classname, classname);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // コントローラ作成
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\controller");
        // 雛形から複製
        sourcePath = Paths
                .get(Svn_Path + "\\main\\java\\kai9\\tmpl\\controller\\" + src_classname + "_Controller.java");
        targetPath = Paths.get(OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\controller\\" + classname
                + "_Controller.java");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(OurDir + "\\Java\\" + packageName1 + "\\" + packageName2 + "\\controller\\" + classname
                + "_Controller.java");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);

        // import(BLOB)
        replaceStr = "";
        if (is_bytea_exist) {
            replaceStr += "import javax.validation.Valid;" + RN;
            replaceStr += "import org.springframework.core.io.ByteArrayResource;" + RN;
            replaceStr += "import org.springframework.core.io.Resource;" + RN;
            replaceStr += "import org.springframework.http.HttpHeaders;" + RN;
            replaceStr += "import org.springframework.http.MediaType;" + RN;
            replaceStr += "import org.springframework.http.ResponseEntity;" + RN;
            replaceStr += "import org.springframework.web.bind.annotation.RequestPart;" + RN;
            replaceStr += "import org.springframework.web.multipart.MultipartFile;" + RN;
        }
        if (!isTargetStrLeave) {
            content = content.replace("//【制御:型】BLOB" + RN, replaceStr);
        }

        // import(Relations)
        replaceStr = "";
        if (!Kai9ComUtils.RelationsList.isEmpty()) {
            List<String> table_names = new ArrayList<>();
            for (Relations Relations : Kai9ComUtils.RelationsList) {
                // テーブル名の末尾から「_a」又は「_b」を取り除く
                String table_name = Relations.tableA;
                if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                    table_name = table_name.substring(0, table_name.length() - 2);
                }
                if (!table_names.contains(table_name)) {
                    replaceStr += "import " + packageName1 + "." + packageName2 + ".model." + table_name + ";" + RN;
                    table_names.add(table_name);
                }
            }
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】relation型", "【制御:終了】relation型",
                isTargetStrLeave);

        // プライマリキー記憶
        IDs = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.columnname.equals("modify_count")) {
                continue;
            } // modify_countは無視
            if (!Models.is_pk) {
                continue;
            }
            if (IDs.equals("")) {
                IDs = IDs + Models.columnname;
            } else {
                IDs = IDs + "," + Models.columnname;
            }
        }
        // PK制御
        IDs1 = "";
        IDs2 = "";
        IDs3 = "";
        String modify_count = "modify_count";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            String ColumnName = Models.columnname.substring(0, 1).toUpperCase() + Models.columnname.substring(1);// 1文字目を大文字変換

            // modify_count制御(親子関係がある場合、親と子で制御方法を変える)
            if (Models.columnname.equals("modify_count")) {
                modify_count = Models.columnname;
            }

            if (Models.columnname.equals("modify_count"))
                continue;
            if (!Models.is_pk)
                continue;
            if (IDs1.equals("")) {
                IDs1 = Models.columnname + " = ?";
                IDs2 = classname + "_request" + ".get" + ColumnName + "()";
                IDs3 = "\"" + ColumnName + "=\" + " + classname + "_request" + ".get" + ColumnName + "()";
            } else {
                IDs1 = IDs1 + " and " + Models.columnname + " = ?";
                IDs2 = IDs2 + "," + classname + "_request" + ".get" + ColumnName + "()";
                IDs3 = IDs3 + "、" + "\"" + ColumnName + "=\" + " + classname + "_request" + ".get" + ColumnName + "()";
            }
        }
        // 新規登録
        targetStr = "select modify_count from single_table_a where s_pk = ?";
        replaceStr = "select " + modify_count + " from " + classname + "_a where " + IDs1;
        content = content.replace(targetStr, replaceStr);

        targetStr = "String.class, single_table_request.getS_pk()";
        replaceStr = "String.class, " + IDs2;
        content = content.replace(targetStr, replaceStr);

        targetStr = "+ \"S_pk=\" + single_table_request.getS_pk() +";
        replaceStr = "+ " + IDs3 + " +";
        content = content.replace(targetStr, replaceStr);

        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            String ColumnName = Models.columnname.substring(0, 1).toUpperCase() + Models.columnname.substring(1);// 1文字目を大文字変換
            if (!Models.is_pk)
                continue;
            replaceStr += "            json.Add(\"" + Models.columnname + "\", String.valueOf(" + classname + "_result.get" + ColumnName + "()));" + RN;
        }
        // 末尾の改行をカット
        if (replaceStr.endsWith("\r\n")) {
            replaceStr = replaceStr.substring(0, replaceStr.length() - 2);
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】カラム:新規登録", "【制御:終了】カラム:新規登録",
                isTargetStrLeave);

        // 件数を返す----------------------------------------------------------
        replaceStr = "";
        boolean IsFirst = true;
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (classname.equals("m_user") & Models.columnname.equals("password"))
                continue;// ユーザマスタ専用(passwordを除く)
            if (Models.columnname.equals("modify_count"))
                continue;// 更新回数は除く

            // 複数の検索候補を、あいまい検索するSQLの作成
            String addStr = "";
            if (!IsFirst) {
                addStr = "                    + \" or\"" + RN;
                replaceStr += addStr;
            }

            if (Models.data_type.contains("smallint") || Models.data_type.contains("integer")
                    || Models.data_type.contains("bigint") || Models.data_type.contains("real")
                    || Models.data_type.contains("double precision") || Models.data_type.contains("numeric")
                    || Models.data_type.contains("smallserial") || Models.data_type.contains("serial")
                    || Models.data_type.contains("bigserial")) {
                // 数値型
                replaceStr += "                    + \" CAST(" + lTable_Name + "." + Models.columnname
                        + " AS TEXT) ~~* any(array[\" + str + \"])\"" + RN;
            } else if (Models.data_type.contains("text") || Models.data_type.contains("varchar")
                    || Models.data_type.contains("character") || Models.data_type.contains("character varying")) {
                // 文字列型
                replaceStr += "                    + \" " + lTable_Name + "." + Models.columnname + " ~~* any(array[\" + str + \"])\"" + RN;
            } else if (Models.data_type.contains("timestamp")) {
                // 日時型
                replaceStr += "                    + \" TO_CHAR(" + lTable_Name + "." + Models.columnname
                        + " , 'YYYY/MM/DD HH24:MI:SS') ~~* any(array[\" + str + \"])\"" + RN;
            } else if (Models.data_type.contains("date")) {
                // 日付型
                replaceStr += "                    + \" TO_CHAR(" + lTable_Name + "." + Models.columnname
                        + " , 'YYYY/MM/DD') ~~* any(array[\" + str + \"])\"" + RN;
            } else if (Models.data_type.contains("time")) {
                // 時刻型
                replaceStr += "                    + \" TO_CHAR(" + lTable_Name + "." + Models.columnname
                        + " , 'HH24:MI:SS') ~~* any(array[\" + str + \"])\"" + RN;
            } else if (Models.data_type.contains("boolean") || Models.data_type.contains("bytea")) {
                // 検索対象外
                if (!IsFirst) {
                    // 追加文字の取り消し
                    replaceStr = replaceStr.substring(0, replaceStr.length() - addStr.length());
                }
                continue;
            } else {
                // その他
                // 検索対象外
                if (!IsFirst) {
                    // 追加文字の取り消し
                    replaceStr = replaceStr.substring(0, replaceStr.length() - addStr.length());
                }
                continue;
            }
            IsFirst = false;

        }
        // 末尾の改行をカット
        if (replaceStr.endsWith("\r\n")) {
            replaceStr = replaceStr.substring(0, replaceStr.length() - 2);
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】カラム:件数を返す", "【制御:終了】カラム:件数を返す",
                isTargetStrLeave);

        // 検索(ページネーション対応----------------------------------------------------------
        // プライマリキー記憶
        String IDs6 = "";
        String IDs5 = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.columnname.equals("modify_count")) {
                continue;
            } // modify_countは無視
            if (!Models.is_pk) {
                continue;
            }
            if (IDs6.equals("")) {
                IDs6 = IDs6 + Models.columnname + " asc";
                IDs5 = IDs5 + CnvType(Models.data_type) + " " + Models.columnname;
            } else {
                IDs6 = IDs6 + "," + Models.columnname + " asc";
                IDs5 = IDs5 + "," + CnvType(Models.data_type) + " " + Models.columnname;
            }
        }
        String replaceStr1 = "";
        String replaceStr2 = "";
        String fields = "";
        if (is_bytea_exist) {
            for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
                Models Models = Kai9ComUtils.modelList.get(i);
                if (Models.data_type.equals("bytea")) {
                    if (fields.equals("")) {
                        fields = fields + "null";
                    } else {
                        fields = fields + ",null";
                    }
                } else {
                    if (fields.equals("")) {
                        fields = fields + Models.columnname;
                    } else {
                        fields = fields + "," + Models.columnname;
                    }
                }
            }
            replaceStr1 += "String sql = \"SELECT single_table_a.* \"," + fields + "//BLOBは個別に取り出すのでnullに挿げ替える";
            replaceStr2 += "order by " + IDs6 + " limit ? offset ?\";" + RN;
            targetStr = "String sql = \"SELECT single_table_a.* \"";
            content = content.replace(targetStr, replaceStr1);
        } else {
            replaceStr2 += "order by " + IDs6 + " limit ? offset ?\";" + RN;
        }
        targetStr = "order by s_pk asc limit ? offset ?\";" + RN;
        content = content.replace(targetStr, replaceStr2);

        replaceStr = "";
        if (is_bytea_exist) {
            replaceStr += "	         //BLOBは個別に取り出すのでnullに挿げ替える" + RN;
            replaceStr += "	         String sql = \"select " + fields + " from " + classname
                    + "_a where \" + Delflg + \" (\"";
        } else {
            replaceStr += "	         String sql = \"select * from " + classname + "_a where \" + Delflg + \" (\"";
        }
        targetStr = "	         String sql = \"SELECT * FROM single_table_a where \" + Delflg + \" (\"";
        content = content.replace(targetStr, replaceStr);

        replaceStr = "";
        IsFirst = true;
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.columnname.equals("modify_count"))
                continue;// 更新回数は除く

            // 複数の検索候補を、あいまい検索するSQLの作成
            String addStr = "                    + \" or\"" + RN;
            if (!IsFirst) {
                replaceStr += addStr;
            }

            if (Models.data_type.contains("smallint") || Models.data_type.contains("integer")
                    || Models.data_type.contains("bigint") || Models.data_type.contains("real")
                    || Models.data_type.contains("double precision") || Models.data_type.contains("numeric")
                    || Models.data_type.contains("smallserial") || Models.data_type.contains("serial")
                    || Models.data_type.contains("bigserial")) {
                // 数値型
                replaceStr += "                    + \" CAST(" + lTable_Name + '.' + Models.columnname
                        + " AS TEXT) ~~* any(array[\" + str + \"])\"" + RN;
            } else if (Models.data_type.contains("text") || Models.data_type.contains("varchar")
                    || Models.data_type.contains("character") || Models.data_type.contains("character varying")) {
                // 文字列型
                replaceStr += "                    + \" " + lTable_Name + '.' + Models.columnname
                        + " ~~* any(array[\" + str + \"])\"" + RN;
            } else if (Models.data_type.contains("timestamp")) {
                // 日時型
                replaceStr += "                    + \" TO_CHAR(" + lTable_Name + '.' + Models.columnname
                        + ", 'YYYY/MM/DD HH24:MI:SS') ~~* any(array[\" + str + \"])\"" + RN;
            } else if (Models.data_type.contains("date")) {
                // 日付型
                replaceStr += "                    + \" TO_CHAR(" + lTable_Name + '.' + Models.columnname
                        + ", 'YYYY/MM/DD') ~~* any(array[\" + str + \"])\"" + RN;
            } else if (Models.data_type.contains("time")) {
                // 時刻型
                replaceStr += "                    + \" TO_CHAR(" + lTable_Name + '.' + Models.columnname
                        + ", 'HH24:MI:SS') ~~* any(array[\" + str + \"])\"" + RN;
            } else if (Models.data_type.contains("boolean") || Models.data_type.contains("bytea")) {
                // 検索対象外
                if (!IsFirst) {
                    // 追加文字の取り消し
                    replaceStr = replaceStr.substring(0, replaceStr.length() - addStr.length());
                }
                continue;
            } else {
                // その他
                // 検索対象外
                if (!IsFirst) {
                    // 追加文字の取り消し
                    replaceStr = replaceStr.substring(0, replaceStr.length() - addStr.length());
                }
                continue;
            }

            IsFirst = false;
        }

        // 末尾の改行をカット
        if (replaceStr.endsWith("\r\n")) {
            replaceStr = replaceStr.substring(0, replaceStr.length() - 2);
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】カラム:ページネーション検索",
                "【制御:終了】カラム:ページネーション検索", isTargetStrLeave);

        targetStr = "order by s_pk limit :limit offset :offset";
        replaceStr = "order by " + IDs + " limit :limit offset :offset";
        content = content.replace(targetStr, replaceStr);

        // 履歴検索----------------------------------------------------------
        // PK制御
        String IDs4 = "";
        IDs5 = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.columnname.equals("modify_count"))
                continue;
            if (!Models.is_pk)
                continue;
            if (IDs4.equals("")) {
                IDs4 = Models.columnname + " = :" + Models.columnname;
                IDs5 = IDs5 + CnvType(Models.data_type) + " " + Models.columnname;
            } else {
                IDs4 = IDs4 + " and " + Models.columnname + " = :" + Models.columnname;
                IDs5 = IDs5 + "," + CnvType(Models.data_type) + " " + Models.columnname;
            }
        }
        fields = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.data_type.equals("bytea")) {
                if (fields.equals("")) {
                    fields = fields + "null";
                } else {
                    fields = fields + ",null";
                }
            } else {
                if (fields.equals("")) {
                    fields = fields + Models.columnname;
                } else {
                    fields = fields + "," + Models.columnname;
                }
            }
        }
        replaceStr1 = "";
        replaceStr2 = "";
        if (is_bytea_exist) {
            replaceStr1 += "//BLOBは個別に取り出すのでnullに挿げ替える" + RN;
            replaceStr1 += "select " + fields;
            replaceStr2 += "where " + IDs4 + " order by " + modify_count + " desc";
        } else {
            replaceStr1 += "select " + classname + "_b.*";
            replaceStr2 += "where " + IDs4 + " order by " + src_classname + "_b." + modify_count + " desc";
        }
        targetStr = "SELECT single_table_b.*  ";
        content = content.replace(targetStr, replaceStr1);
        targetStr = "where s_pk = :s_pk order by single_table_b.modify_count desc";
        content = content.replace(targetStr, replaceStr2);

        content = content.replace("String single_table_history_find(Integer s_pk, HttpServletResponse res)",
                "String " + classname + "_history_find(" + IDs5 + ", HttpServletResponse res)");

        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.columnname.equals("modify_count"))
                continue;
            if (!Models.is_pk)
                continue;
            replaceStr += "                .addValue(\"" + Models.columnname + "\", " + Models.columnname + ");" + RN;
        }
        // 末尾の改行をカット
        if (replaceStr.endsWith("\r\n")) {
            replaceStr = replaceStr.substring(0, replaceStr.length() - 2);
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】カラム:履歴を取得する", "【制御:終了】カラム:履歴を取得する",
                isTargetStrLeave);

        // relationSQL①----------------------------------------------------------
        // 置換候補をリストアップ
        replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a", relation.tableA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", relation.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", relation.columnB));
        }
        startMarker = "【制御:開始】relationSQL①";
        endMarker = "【制御:終了】relationSQL①";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // relationSQL②----------------------------------------------------------
        // 置換候補をリストアップ
        replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a.related_pk",
                    relation.tableA + '.' + relation.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("single_table_a.related_pk",
                    lTable_Name + '.' + relation.src_column));
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a", relation.tableA));
            replacements.add(new AbstractMap.SimpleEntry<>(src_classname, classname));
        }
        startMarker = "【制御:開始】relationSQL②";
        endMarker = "【制御:終了】relationSQL②";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // relationSQL③----------------------------------------------------------
        replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a.related_pk",
                    relation.tableA + '.' + relation.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("single_table_a.related_pk",
                    lTable_Name + '.' + relation.src_column));
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a", relation.tableA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", relation.columnB));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", relation.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>(src_classname, classname));
        }
        startMarker = "【制御:開始】relationSQL③";
        endMarker = "【制御:終了】relationSQL③";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // relationSQL④----------------------------------------------------------
        replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a.related_pk",
                    relation.tableA + '.' + relation.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("single_table_a.related_pk",
                    lTable_Name + '.' + relation.src_column));
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a", relation.tableA));
            replacements.add(new AbstractMap.SimpleEntry<>(src_classname, classname));
        }
        startMarker = "【制御:開始】relationSQL④";
        endMarker = "【制御:終了】relationSQL④";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // relationSQL⑤----------------------------------------------------------
        replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            // Bテーブル名を確保
            String table_name_b = relation.tableB;
            if (table_name_b.endsWith("_a") || table_name_b.endsWith("_b")) {
                table_name_b = table_name_b.substring(0, table_name_b.length() - 2) + "_b";
            }
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_b", table_name_b));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", relation.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", relation.columnB));
        }
        startMarker = "【制御:開始】relationSQL⑤";
        endMarker = "【制御:終了】relationSQL⑤";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // relationSQL⑥----------------------------------------------------------
        ArrayList<Object> table_names = new ArrayList<>();
        replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            if (table_names.contains(relation.tableA))
                continue;
            table_names.add(relation.tableA);

            String table_name = lTable_Name;
            if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                table_name = table_name.substring(0, table_name.length() - 2);
            }
            String tableA = relation.tableA;
            if (tableA.endsWith("_a") || tableA.endsWith("_b")) {
                tableA = tableA.substring(0, tableA.length() - 2);
            }

            replacements.add(
                    new AbstractMap.SimpleEntry<>("related_table_b.related_pk", tableA + "_b." + relation.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("single_table_b.related_pk",
                    table_name + "_b." + relation.src_column));
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_b", tableA + "_b"));
            replacements.add(new AbstractMap.SimpleEntry<>(src_classname, classname));
        }
        startMarker = "【制御:開始】relationSQL⑥";
        endMarker = "【制御:終了】relationSQL⑥";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // relationSQL⑦----------------------------------------------------------
        table_names = new ArrayList<>();
        replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            if (table_names.contains(relation.tableA))
                continue;
            table_names.add(relation.tableA);

            String table_name = lTable_Name;
            if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                table_name = table_name.substring(0, table_name.length() - 2);
            }
            String tableA = relation.tableA;
            if (tableA.endsWith("_a") || tableA.endsWith("_b")) {
                tableA = tableA.substring(0, tableA.length() - 2);
            }

            replacements.add(
                    new AbstractMap.SimpleEntry<>("related_table_b.related_pk", tableA + "_b." + relation.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("single_table_b.related_pk",
                    table_name + "_b." + relation.src_column));
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_b", tableA + "_b"));
            replacements.add(new AbstractMap.SimpleEntry<>(src_classname, classname));
        }
        startMarker = "【制御:開始】relationSQL⑦";
        endMarker = "【制御:終了】relationSQL⑦";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // relationSQL⑧----------------------------------------------------------
        replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a.related_data",
                    relation.tableB + '.' + relation.columnB));
        }
        startMarker = "【制御:開始】relationSQL⑧";
        endMarker = "【制御:終了】relationSQL⑧";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // relationSQL⑨----------------------------------------------------------
        replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a.related_data",
                    relation.tableB + '.' + relation.columnB));
        }
        startMarker = "【制御:開始】relationSQL⑨";
        endMarker = "【制御:終了】relationSQL⑨";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // relationSQL⑩----------------------------------------------------------
        replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            replacements.add(new AbstractMap.SimpleEntry<>("left join related_table_a",
                    "left join " + relation.tableB));
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a.related_pk",
                    relation.tableA + '.' + relation.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("single_table_a.related_pk",
                    lTable_Name + '.' + relation.src_column));
        }
        startMarker = "【制御:開始】relationSQL⑩";
        endMarker = "【制御:終了】relationSQL⑩";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // relation
        // 一覧を返す(選択リストボックス用)----------------------------------------------------------
        replacements = new ArrayList<>();
        for (Relations relation : Kai9ComUtils.RelationsList) {
            // テーブル名の末尾から「_a」又は「_b」を取り除く
            // テーブル名の末尾から「_a」又は「_b」を取り除く
            String table_name = relation.tableA;
            if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                table_name = table_name.substring(0, table_name.length() - 2);
            }
            replacements.add(new AbstractMap.SimpleEntry<>("related_table", table_name));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", relation.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", relation.columnB));
            replacements.add(new AbstractMap.SimpleEntry<>("Related_pk",
                    Character.toUpperCase(relation.columnA.charAt(0)) + relation.columnA.substring(1))); // Getter
            replacements.add(new AbstractMap.SimpleEntry<>("Related_data",
                    Character.toUpperCase(relation.columnB.charAt(0)) + relation.columnB.substring(1))); // Getter
        }
        startMarker = "【制御:開始】relation";
        endMarker = "【制御:終了】relation";
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // 置換
        content = content.replace(src_classname, classname);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // パッケージ名のgrep置換
        String directoryPath = OurDir + "\\Java";
        List<String> fileExtensions = List.of(".java");
        grepReplaceInDirectory(directoryPath, fileExtensions, "kai9.tmpl.", packageName1 + "." + packageName2 + ".");
        grepReplaceInDirectory(directoryPath, fileExtensions, "package kai9", "package " + packageName1);

    }

    /**
     * 指定されたディレクトリ内のファイルに対して、特定の文字列を検索し置換を行うメソッド
     * 
     * @param directoryPath 探索するディレクトリのパス
     * @param fileExtensions 対象とするファイルの拡張子のリスト
     * @param searchString 検索する文字列
     * @param replaceString 置換後の文字列
     */
    public static void grepReplaceInDirectory(String directoryPath, List<String> fileExtensions, String searchString,
            String replaceString) {
        try {
            try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) { // ディレクトリを再帰的に探索し、全てのファイルパスを取得
                paths.filter(Files::isRegularFile) // 通常のファイルのみを対象とする
                        .filter(p -> fileExtensions.stream().anyMatch(ext -> p.toString().endsWith(ext))) // 指定された拡張子のファイルのみをフィルタリング
                        .forEach(filePath -> {
                            try {
                                // ファイル内容を全て読み込み（UTF-8エンコーディングを指定）
                                String content = Files.readString(filePath);
                                // 指定された文字列を置換
                                String newContent = content.replace(searchString, replaceString);

                                // 置換が発生した場合のみファイルを書き換え
                                if (!content.equals(newContent)) {
                                    Files.writeString(filePath, newContent);
                                }
                            } catch (IOException e) {
                                Kai9Util.ErrorMsg(String.join("", Kai9Utils.GetException(e)));
                            }
                        });
            }
        } catch (Exception e) {
            Kai9Util.ErrorMsg(String.join("", Kai9Utils.GetException(e)));
        }
    }

    /**
     * 型マッピング(PostgreSQL→Java) https://beanql.osdn.jp/type_map.html
     */
    public static String CnvType(String data_type) {
        String lDataType2 = "型不明";

        // 桁数指定がある型は、桁数箇所を取り除く
        if (data_type.contains("varchar")) {
            data_type = "varchar";
        } else if (data_type.contains("character")) {
            data_type = "character";
        } else if (data_type.contains("char")) {
            data_type = "character";
        } else if (data_type.contains("numeric")) {
            data_type = "numeric";
        }

        // 型変換
        switch (data_type) {
        case "boolean":
            lDataType2 = "Boolean";
            break;
        case "smallint":
            lDataType2 = "Short";
            break;
        case "integer":
            lDataType2 = "Integer";
            break;
        case "bigint":
            lDataType2 = "Long";
            break;
        case "real":
            lDataType2 = "Float";
            break;
        case "double precision":
            lDataType2 = "Double";
            break;
        case "numeric":
            lDataType2 = "java.math.BigDecimal";
            break;
        case "text":
            lDataType2 = "String";
            break;
        case "varchar":
            lDataType2 = "String";
            break;
        case "character":
            lDataType2 = "String";
            break;
        case "bytea":
            lDataType2 = "byte[]";
            break;
        case "timestamp":
            lDataType2 = "java.sql.Timestamp";
            break;
        case "date":
            lDataType2 = "java.sql.Date";
            break;
        case "time":
            lDataType2 = "java.sql.Time";
            break;
        case "smallserial":
            lDataType2 = "Short";
            break;
        case "serial":
            lDataType2 = "Integer";
            break;
        case "bigserial ":
            lDataType2 = "Long";
            break;

        // 以下の型は、自分で使う機会が無いので省略した(汎用的に作る場合、これらの実装とテストが必要)
        // https://www.npgsql.org/doc/types/basic.html
        // money decimal
        // citext string
        // json string
        // jsonb string
        // xml string
        // uuid Guid
        // timestamp with time zone DateTime (Utc1)
        // time with time zone DateTimeOffset
        // interval TimeSpan3
        // cidr (IPAddress, int)
        // inet IPAddress
        // macaddr PhysicalAddress
        // tsquery NpgsqlTsQuery
        // tsvector NpgsqlTsVector
        // bit(1) bool
        // bit(n) BitArray
        // bit varying BitArray
        // point NpgsqlPoint
        // lseg NpgsqlLSeg
        // path NpgsqlPath
        // polygon NpgsqlPolygon
        // line NpgsqlLine
        // circle NpgsqlCircle
        // box NpgsqlBox
        // hstore Dictionary<string, string>
        // oid uint
        // xid uint
        // cid uint
        // oidvector uint[]
        // name string
        // (internal) char char
        // geometry (PostGIS) PostgisGeometry
        // record object[]
        // composite types T
        // range types NpgsqlRange<TElement>
        // multirange types (PG14) NpgsqlRange<TElement>[]
        // enum types TEnum
        // array types Array (of element type)
        }
        return lDataType2;
    }

    /**
     * 文字列を操作して特定の行を削除または置換する関数
     * 
     * @param input 元の文字列
     * @param 特定の行とその次の行を削除するかどうかのフラグ
     * @return 編集結果の文字列
     */
    public static String manipulateString(String input, String targetStr, boolean conditionA) {
        targetStr += ".*?\\r\\n";
        if (conditionA) {
            // 特定の行のみを削除する
            String regex = targetStr;
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(input);
            return matcher.replaceAll("");
        } else {
            // 特定の行とその次の行を削除する
            String regex = targetStr + ".*?\\r\\n";
            Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(input);
            return matcher.replaceFirst("");
        }
    }

}
