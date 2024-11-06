package kai9.com.srcmake;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Sheet;

import kai9.libs.Kai9Util;
import kai9.libs.Kai9Utils;
import kai9.libs.PoiUtil;
import kai9.com.common.Kai9comUtils;
import kai9.com.srcmake.ScenarioMaker.LengthPattern;

public class ReactMaker {

    // 改行コード
    private static String RN = "\r\n";

    // エラー格納リスト
    private static List<String> Errors = new ArrayList<>();

    public static boolean Make(Sheet pWs, String OurDir, String Svn_Path, boolean isTargetStrLeave) throws IOException {

        boolean IsError = false;
        // ======================================================================
        // DB定義書取込(ヘッダ)
        // ======================================================================
        // "論理名称(和名
        int lRow1 = PoiUtil.findRow(pWs, "#R1#");
        if (lRow1 == -1) {
            Errors.add("制御文字「#R1#」が見つかりません" + RN);
            IsError = true;
        }
        int lCol_TablleName_J = PoiUtil.findCol(pWs, lRow1, "テーブル名(和名)") + 2;
        String lTable_Name_J = PoiUtil.GetCellValue(pWs, lRow1, lCol_TablleName_J);

        // "テーブル名
        int lRow2 = PoiUtil.findRow(pWs, "#R2#");
        int lCol_TablleName = PoiUtil.findCol(pWs, lRow2, "テーブル名") + 2;
        String lTable_Name = PoiUtil.GetCellValue(pWs, lRow2, lCol_TablleName);

        // カラム番号を特定
        int lTitleRow = PoiUtil.findRow(pWs, "#R4#");
        if (lTitleRow == -1) {
            Errors.add("制御文字「#R4#」が見つかりません" + RN);
            IsError = true;
        }
        if (IsError) {
            // 制御文字でエラーがある場合、読み取りが出来ないので、エラー表示して抜ける
            Kai9Util.ErrorMsg(String.join("", Errors));
            return false;
        }

        // ======================================================================
        // DB定義書のバリデーションチェック(ヘッダ箇所)
        // ======================================================================
        // テーブル名が定義されている事
        if (lTable_Name.equals("")) {
            Errors.add("【テーブル名】 は省略できません。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        // "履歴テーブル名が定義されている事
        int lRow3 = PoiUtil.findRow(pWs, "#R3#");
        int lCol_TablleNameB = PoiUtil.findCol(pWs, lRow3, "履歴テーブル名") + 2;
        String lTable_NameB = PoiUtil.GetCellValue(pWs, lRow3, lCol_TablleNameB);
        if (lTable_NameB.equals("")) {
            Errors.add("【歴テーブル名】 は省略できません。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        // テーブル名が半角英数とアンダースコアだけである事
        String HAS_HALF_ALPHANUMERIC_UNSCO = "^[0-9a-zA-Z_]+$";
        if (!lTable_Name.matches(HAS_HALF_ALPHANUMERIC_UNSCO)) {
            Errors.add("【テーブル名】 に半角英数、又はアンダースコア以外の文字が含まれています。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        // 履歴テーブル名が半角英数とアンダースコアだけである事
        if (!lTable_NameB.matches(HAS_HALF_ALPHANUMERIC_UNSCO)) {
            Errors.add("【履歴テーブル名】 から半角英数、又はアンダースコア以外の文字を取り除いて下さい。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        // テーブル名の末尾が「_a」である事
        if (!lTable_Name.substring(lTable_Name.length() - 2).equals("_a")) {
            Errors.add("【テーブル名】 の末尾は「_a」にして下さい。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        // 履歴テーブルの末尾が「_b」である事
        if (!lTable_NameB.substring(lTable_NameB.length() - 2).equals("_b")) {
            Errors.add("【履歴テーブル】 の末尾は「_b」にして下さい。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (IsError) {
            // ヘッダ箇所でエラーがある場合、明細の読み取りが出来ないので、エラー表示して抜ける
            Kai9Util.ErrorMsg(String.join("", Errors));
            return false;
        }

        // ======================================================================
        // DB定義書取込(明細)
        // ======================================================================
        int lCol_No = PoiUtil.findCol(pWs, lTitleRow, "No");
        int lCol_FieldName_J = PoiUtil.findCol(pWs, lTitleRow, "カラム名(和名)");
        int lCol_FieldName = PoiUtil.findCol(pWs, lTitleRow, "カラム名");
        int lCol_DataType = PoiUtil.findCol(pWs, lTitleRow, "データ型");
        int lCol_Digits = PoiUtil.findCol(pWs, lTitleRow, "桁数");
        int lCol_DefaultValue = PoiUtil.findCol(pWs, lTitleRow, "初期値");
        int lCol_IsPK = PoiUtil.findCol(pWs, lTitleRow, "PK");
        int lCol_IsNN = PoiUtil.findCol(pWs, lTitleRow, "必須");
        int lCol_unique_index = PoiUtil.findCol(pWs, lTitleRow, "ユニークINDEX");
        int lCol_is_mod_admin = PoiUtil.findCol(pWs, lTitleRow, "編集(管理者)");
        int lCol_is_mod_normal = PoiUtil.findCol(pWs, lTitleRow, "編集(一般)");
        int lCol_is_mod_readonly = PoiUtil.findCol(pWs, lTitleRow, "編集(参照専用)");
        int lCol_MinLength = PoiUtil.findCol(pWs, lTitleRow, "最小桁数");
        int lCol_default_value = PoiUtil.findCol(pWs, lTitleRow, "WEB初期値");
        int lCol_input_type = PoiUtil.findCol(pWs, lTitleRow, "INPUT TYPE");
        int lCol_validation_check = PoiUtil.findCol(pWs, lTitleRow, "バリデーションチェック");
        int lCol_special_control = PoiUtil.findCol(pWs, lTitleRow, "特殊制御");

        // 明細の目印になる列見出し見つからない場合エラー
        if (lCol_No == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【No】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_FieldName_J == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【カラム名(和名)】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_FieldName == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【カラム名】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_DataType == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【データ型】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_Digits == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【桁数】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_DefaultValue == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【初期値】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_IsPK == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【PK】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_IsNN == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【必須】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_unique_index == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【ユニークINDEX】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_is_mod_admin == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【編集(管理者)】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_is_mod_normal == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【編集(一般)】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_is_mod_readonly == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【編集(参照専用)】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_input_type == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【INPUT TYPE】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_validation_check == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【バリデーションチェック】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (lCol_special_control == -1) {
            Errors.add(lTitleRow + "行目に、列見出しの【特殊制御】が見つかりませんでした。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }
        if (IsError) {
            // 明細の見出し箇所でエラーがある場合、明細の読み取りが出来ないので、エラー表示して抜ける
            Kai9Util.ErrorMsg(String.join("", Errors));
            return false;
        }

        boolean is_bytea_exist = false;// BLOBの存在確認用
        short id_count = 0;// 主キー数
        int lTopRow = PoiUtil.findRow(pWs, "#R5#");
        Kai9ComUtils.modelList.clear();
        Kai9ComUtils.RelationsList.clear();

        for (int Rownum = lTopRow; Rownum < pWs.getLastRowNum(); Rownum++) {
            if (PoiUtil.GetStringValue(pWs, Rownum, lCol_No).equals("")) {
                continue;
            }
            String data_type = PoiUtil.GetStringValue(pWs, Rownum, lCol_DataType).toLowerCase();
            // BLOBの存在確認
            if (data_type.equals("bytea")) is_bytea_exist = true;
            // 主キー数をカウント
            String columnName = PoiUtil.GetStringValue(pWs, Rownum, lCol_FieldName).toLowerCase();
            boolean is_pk = false;
            if (PoiUtil.GetStringValue(pWs, Rownum, lCol_IsPK).contentEquals("〇") ||
                    PoiUtil.GetStringValue(pWs, Rownum, lCol_IsPK).contentEquals("○")) {
                is_pk = true;
                if (!columnName.equals("modify_count")) {// modify_countは無視
                    id_count++;

                    if (!data_type.equals("serial") && !data_type.equals("smallserial") && !data_type.equals("bigserial")) {
                        // PKが自動採番型以外の場合エラー
                        Errors.add("PKは、自動採番型しか指定出来ません(serial/smallserial/bigserial)。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、指定された型=" + data_type + RN);
                        IsError = true;
                    }
                }
            }

            // not null
            boolean is_not_null = false;
            if (PoiUtil.GetStringValue(pWs, Rownum, lCol_IsNN).contentEquals("NOT NULL")) {
                is_not_null = true;
            }
            // 桁数
            int MaxLength = PoiUtil.GetNumericValue(pWs, Rownum, lCol_Digits).intValue();

            // カラム名(和名)
            String FieldName_J = PoiUtil.GetStringValue(pWs, Rownum, lCol_FieldName_J);
            // 特殊制御
            String special_control_tmp = PoiUtil.GetStringValue(pWs, Rownum, lCol_special_control);
            // 特殊制御(リレーション
            String special_control_relation = "";
            if (special_control_tmp.contains("【relation】")) {
                special_control_relation = "【relation】" + special_control_tmp.split("【relation】")[1].split("【")[0];
            }
            // 特殊制御(最大数値
            String special_control_max_tmp = "";
            if (special_control_tmp.contains("【最大数値】")) {
                special_control_max_tmp = "【最大数値】" + special_control_tmp.split("【最大数値】")[1].split("【")[0];
            }
            double special_control_max = 0;
            if (!special_control_max_tmp.isEmpty()) {
                if (special_control_max_tmp.contains("【最大数値】")) {
                    // "【最大数値】"の後の部分を抽出する
                    String maxPart = special_control_max_tmp.split("【最大数値】")[1].split("【")[0];
                    // 抽出した部分から数値（小数点も含む）を取り出す
                    String maxNumberStr = maxPart.replaceAll("[^0-9.]", "");
                    // 数値部分が空でない場合はdoubleに変換し、max_valueに設定する。空の場合は0に設定する。
                    special_control_max = maxNumberStr.isEmpty() ? 0.0 : Double.parseDouble(maxNumberStr);
                } else {
                    // "【最大数値】"が含まれていない場合はmax_valueを0に設定する
                    special_control_max = 0;
                }
            }
            // 特殊制御(最小数値
            String special_control_min_tmp = "";
            if (special_control_tmp.contains("【最小数値】")) {
                special_control_min_tmp = "【最小数値】" + special_control_tmp.split("【最小数値】")[1].split("【")[0];
            }
            double special_control_min = 0;
            if (!special_control_min_tmp.isEmpty()) {
                if (special_control_min_tmp.contains("【最小数値】")) {
                    // "【最小数値】"の後の部分を抽出する
                    String minPart = special_control_min_tmp.split("【最小数値】")[1].split("【")[0];
                    // 抽出した部分から数値（小数点も含む）を取り出す
                    String minNumberStr = minPart.replaceAll("[^0-9.]", "");
                    // 数値部分が空でない場合はdoubleに変換し、min_valueに設定する。空の場合は0に設定する。
                    special_control_min = minNumberStr.isEmpty() ? 0.0 : Double.parseDouble(minNumberStr);
                } else {
                    // "【最小数値】"が含まれていない場合はmin_valueを0に設定する
                    special_control_min = 0;
                }
            }

            // ユニークインデックス
            String unique_index = PoiUtil.GetStringValue(pWs, Rownum, lCol_unique_index);
            // 編集(管理者)
            String is_mod_admin = PoiUtil.GetStringValue(pWs, Rownum, lCol_is_mod_admin);
            // 編集(一般)
            String is_mod_normal = PoiUtil.GetStringValue(pWs, Rownum, lCol_is_mod_normal);
            // 編集(参照専用)
            String is_mod_readonly = PoiUtil.GetStringValue(pWs, Rownum, lCol_is_mod_readonly);
            // 最小桁数
            String MinLength = removeDecimalPart(PoiUtil.GetStringValue(pWs, Rownum, lCol_MinLength));
            // WEB初期値
            String default_value = PoiUtil.GetStringValue(pWs, Rownum, lCol_default_value);
            // INPUT TYPE
            String input_type = PoiUtil.GetStringValue(pWs, Rownum, lCol_input_type);
            // バリデーションチェック
            String validation_check = PoiUtil.GetStringValue(pWs, Rownum, lCol_validation_check);

            // ======================================================================
            // DB定義書のバリデーションチェック(明細箇所)
            // ======================================================================
            // PKとシステムカラムの場合、編集は「不可固定」しか許容しない
            if (is_pk || columnName.equals("modify_count") || columnName.equals("update_u_id") || columnName.equals("update_date")) {
                if (!is_mod_admin.equals("不可固定")) {
                    Errors.add("PK及びKai9必須カラムの場合、【編集(管理者)】 には「不可固定」しか入力出来ません。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + is_mod_admin + RN);
                    IsError = true;
                }
                if (!is_mod_normal.equals("不可固定")) {
                    Errors.add("PK及びKai9必須カラムの場合、【編集(一般)】 には「不可固定」しか入力出来ません。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + is_mod_normal + RN);
                    IsError = true;
                }
                if (!is_mod_readonly.equals("不可固定")) {
                    Errors.add("PK及びKai9必須カラムの場合、【編集(参照専用)】 には「不可固定」しか入力出来ません。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + is_mod_readonly + RN);
                    IsError = true;
                }
            }

            // 編集(管理者)が「可」「不可」「不可固定」だけの入力になっている事
            if (!is_mod_admin.equals("可") && !is_mod_admin.equals("不可") && !is_mod_admin.equals("不可固定")) {
                Errors.add("【編集(管理者)】 には、「可」「不可」「不可固定」の何れかを入力して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + is_mod_admin + RN);
                IsError = true;
            } else {
                // 編集を、true、falseに変換する
                if (is_mod_admin.equals("可")) {
                    is_mod_admin = "true";
                } else if (is_mod_admin.equals("不可") || is_mod_admin.equals("不可固定")) {
                    is_mod_admin = "false";
                }
            }

            // 編集(一般)が「可」「不可」「不可固定」だけの入力になっている事
            if (!is_mod_normal.equals("可") && !is_mod_normal.equals("不可") && !is_mod_normal.equals("不可固定")) {
                Errors.add("【編集(一般)】 には、「可」「不可」「不可固定」の何れかを入力して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + is_mod_normal + RN);
                IsError = true;
            } else {
                // 編集を、true、falseに変換する
                if (is_mod_normal.equals("可")) {
                    is_mod_normal = "true";
                } else if (is_mod_normal.equals("不可") || is_mod_normal.equals("不可固定")) {
                    is_mod_normal = "false";
                }
            }
            // 編集(参照専用)が「可」「不可」「不可固定」だけの入力になっている事
            if (!is_mod_readonly.equals("可") && !is_mod_readonly.equals("不可") && !is_mod_readonly.equals("不可固定")) {
                Errors.add("【編集(参照専用)】 には、「可」「不可」「不可固定」の何れかを入力して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + is_mod_readonly + RN);
                IsError = true;
            } else {
                // 編集を、true、falseに変換する
                if (is_mod_readonly.equals("可")) {
                    is_mod_readonly = "true";
                } else if (is_mod_readonly.equals("不可") || is_mod_readonly.equals("不可固定")) {
                    is_mod_readonly = "false";
                }
            }

            // 最小桁数が-や空欄の場合0に置換する
            if (MinLength.equals("-") || MinLength.isEmpty()) {
                MinLength = "0";
            }
            // 最小桁数に数値以外が入っていないか確認
            try {
                Integer.parseInt(MinLength);
            } catch (NumberFormatException e) {
                Errors.add("【最小桁数】 には数値だけを入力して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + MinLength + RN);
                IsError = true;
            }

            // 独自クラス一時記憶
            Kai9ComUtils.modelList.add(new Models(columnName, data_type, is_pk, is_not_null, MaxLength, FieldName_J, special_control_relation, special_control_min, special_control_max, unique_index,
                    Boolean.parseBoolean(is_mod_admin), Boolean.parseBoolean(is_mod_normal), Boolean.parseBoolean(is_mod_readonly),
                    Integer.parseInt(MinLength), default_value, input_type, validation_check));

            // カラム名(和名)が定義されている事
            if (FieldName_J.equals("")) {
                Errors.add("【カラム名(和名)】 は省略できません。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + RN);
                IsError = true;
            }

            // カラム名が設定されている事
            if (columnName.equals("")) {
                Errors.add("【カラム名】 は省略できません。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + RN);
                IsError = true;
            }
            // データ型が設定されている事
            if (data_type.equals("")) {
                Errors.add("【データ型】 は省略できません。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + RN);
                IsError = true;
            }
            // データ型がKai9でサポートする型じゃなくてもエラーにしない(「型不明」と表記したソースを自動生成する仕様)

            // PKにNOT NULL以外の文字が設定されている場合エラー
            if (!PoiUtil.GetStringValue(pWs, Rownum, lCol_IsNN).contentEquals("") & !PoiUtil.GetStringValue(pWs, Rownum, lCol_IsNN).contentEquals("NOT NULL")) {
                Errors.add("【必須】には「NOT NULL」と記載して下さい。「" + PoiUtil.GetStringValue(pWs, Rownum, lCol_IsNN) + "」は設定できません。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + RN);
                IsError = true;
            }
            // PKにはNOT NULL属性が設定されている事
            if (is_pk & !is_not_null) {
                Errors.add("【PK】が「〇」の場合、【必須】も「NOT NULL」で指定して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + RN);
                IsError = true;
            }

            // ユニークインデックス
            if (!unique_index.isEmpty() && !unique_index.matches("[〇○①②③④⑤⑥⑦⑧⑨⑩-]+")) {
                Errors.add("【ユニークインデックス】 に指定できるのは「〇○①②③④⑤⑥⑦⑧⑨⑩-」だけです。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + unique_index + RN);
                IsError = true;
            }

            // VARCHAR、CHARに桁数が設定されている事
            if (data_type.contains("char") & MaxLength == 0) {
                Errors.add("【桁数】は省略できません。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + RN);
                IsError = true;
            }

            // カラム名が半角英数とアンダースコアだけである事
            if (!columnName.matches(HAS_HALF_ALPHANUMERIC_UNSCO)) {
                Errors.add("【カラム名】 に半角英数、又はアンダースコア以外の文字が含まれています。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + RN);
                IsError = true;
            }

            // INPUT TYPE
            if (!input_type.toUpperCase().equals("TEXTINPUT") && !input_type.toUpperCase().equals("NUMBERINPUT")
                    && !input_type.toUpperCase().equals("TEXTAREA") && !input_type.toUpperCase().equals("SELECTINPUT")
                    && !input_type.toUpperCase().equals("CHECKBOX") && !input_type.toUpperCase().equals("-")) {
                Errors.add("【INPUT TYPE】 に指定できるのは「TextInput/NumberInput/TextArea/SelectInput/CheckBox/-」だけです。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + input_type + RN);
                IsError = true;
            }

            // INPUT TYPEの入力値確認（システムカラム、PKを除く）
            if (!Kai9ComUtils.isSystemColumn(columnName) && !is_pk) {
                // string(100文字未満)
                if (!input_type.toUpperCase().equals("TEXTINPUT") &&
                        (CnvType(data_type).equals("string") && MaxLength < 100 && !data_type.equals("text")
                                || data_type.equals("date")
                                || data_type.equals("timestamp"))
                        && special_control_relation.isEmpty()) {
                    Errors.add("【INPUT TYPE】 には「TextInput」を指定して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + input_type + RN);
                    IsError = true;
                }
                // string(100文字を超える場合、又はtext型)
                if (!input_type.toUpperCase().equals("TEXTAREA") && ((CnvType(data_type).equals("string") && MaxLength > 100) || data_type.equals("text")) && special_control_relation.isEmpty()) {
                    Errors.add("【INPUT TYPE】 には「TextArea」を指定して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + input_type + RN);
                    IsError = true;
                }
                // number
                if (!input_type.toUpperCase().equals("NUMBERINPUT") && CnvType(data_type).equals("number") && special_control_relation.isEmpty()) {
                    Errors.add("【INPUT TYPE】 には「NumberInput」を指定して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + input_type + RN);
                    IsError = true;
                }
                // boolean
                if (!input_type.toUpperCase().equals("CHECKBOX") && CnvType(data_type).equals("boolean") && special_control_relation.isEmpty()) {
                    Errors.add("【INPUT TYPE】 には「CheckBox」を指定して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + input_type + RN);
                    IsError = true;
                }
                // 特殊制御
                if (!input_type.toUpperCase().equals("SELECTINPUT") && !special_control_relation.isEmpty()) {
                    Errors.add("【INPUT TYPE】 には「SelectInput」を指定して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + input_type + RN);
                    IsError = true;
                }
            }
            if (columnName.toLowerCase().equals("delflg") && !input_type.toUpperCase().equals("CHECKBOX")) {
                Errors.add("【INPUT TYPE】 には「CheckBox」を指定して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + input_type + RN);
                IsError = true;
            }

            // バリデーションチェック
            if (!validation_check.toUpperCase().equals("全角限定") && !validation_check.toUpperCase().equals("半角限定") && !validation_check.isEmpty()
                    && !validation_check.toUpperCase().equals("半角英字限定") && !validation_check.toUpperCase().equals("半角数字限定")
                    && !validation_check.toUpperCase().equals("半角記号限定") && !validation_check.toUpperCase().equals("半角カナ限定")
                    && !validation_check.toUpperCase().equals("半角記号限定") && !validation_check.toUpperCase().equals("全角カナ限定")
                    && !validation_check.toUpperCase().equals("数値限定") && !validation_check.toUpperCase().equals("小数点") && !validation_check.toUpperCase().equals("郵便番号")
                    && !validation_check.toUpperCase().equals("電話番号") && !validation_check.toUpperCase().equals("日付") && !validation_check.toUpperCase().equals("日時")
                    && !validation_check.toUpperCase().equals("メールアドレス") && !validation_check.toUpperCase().equals("URL") && !validation_check.toUpperCase().contains("【正規表現】")) {
                Errors.add("【バリデーションチェック】 に指定できるのは「全角限定/半角限定/半角カナ限定/数値限定/小数点/郵便番号/電話番号/日付/日時/メールアドレス/URL/-」だけです。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + "、入力されている文字=" + validation_check + RN);
                IsError = true;
            }

            // 特殊制御(リレーション)
            if (!special_control_relation.isEmpty()) {
                if (
                // 【relation】で始まらない場合
                !special_control_relation.startsWith("【relation】")
                        // .が2つじゃない場合
                        || special_control_relation.chars().filter(ch -> ch == '.').count() != 2
                        // :が1つじゃない場合
                        || special_control_relation.chars().filter(ch -> ch == ':').count() != 1) {
                    // 有効な例) 【relation】related_table_a.related_pk:related_table_a.related_data
                    Errors.add("【特殊制御】が不正です。「【relation】テーブル名.カラム名:テーブル名.カラム名」の構文で記載して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + RN);
                    IsError = true;
                }
                // 中身を分解しモデルへ格納
                Relations Relations = parse_Relations(special_control_relation, columnName);
                if (Relations == null) {
                    Errors.add("【特殊制御】が不正です。「【relation】テーブル名.カラム名:テーブル名.カラム名」の構文で記載して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + RN);
                    IsError = true;
                } else {
                    if (!Relations.tableA.equals(Relations.tableB)) {
                        Errors.add("【特殊制御】が不正です。「【relation】で指定するテーブルは右辺と左辺で同じ物を指定して下さい。シート名=" + pWs.getSheetName() + "、行番号=" + String.valueOf(Rownum + 1) + RN);
                        IsError = true;
                    } else {
                        // リスト内に同じ要素が存在しない場合に追加
                        if (!Kai9ComUtils.RelationsList.contains(Relations)) {
                            Kai9ComUtils.RelationsList.add(Relations);
                        }
                    }
                }
            }

        }

        // PKが１つではない場合
        if (id_count != 1) {
            Errors.add("【PK】は必ず1つだけ指定して下さい(modify_countは除く)。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }

        // クラス名
        String classname;// 全て小文字
        if (lTable_Name.substring(lTable_Name.length() - 2, lTable_Name.length()).equals("_a")) {
            // クラス名では、テーブル名の「_a」をカットする
            classname = lTable_Name.substring(0, lTable_Name.length() - 2);
        } else {
            classname = lTable_Name;
        }
        String CLASSNAME = classname.toUpperCase();// 全て大文字
        String Classname = classname.substring(0, 1).toUpperCase() + classname.substring(1);// 1文字目を大文字変換

        // クラス名_原本
        String src_classname = "single_table";// 全て小文字
        String src_CLASSNAME = "SINGLE_TABLE";// 全て大文字
        String src_Classname = "Single_table";// 1文字目を大文字変換

        String lastCharacter_ClassName = classname.substring(classname.length() - 1);

        boolean IsHitKai9 = false;
        if (!lastCharacter_ClassName.equals("2") & !lastCharacter_ClassName.equals("3")) {// 子、孫は除く、玄孫(4)は使った事が無いので必要なら追加する
            // Kai9必須カラムが存在する事(更新者)
            for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
                Models Models = Kai9ComUtils.modelList.get(i);
                if (!Models.columnname.equals("update_u_id")) continue;
                IsHitKai9 = true;
                break;
            }
            if (!IsHitKai9) {
                Errors.add("【更新者(update_u_id)】はKai9必須カラムなので省略できません。シート名=" + pWs.getSheetName() + RN);
                IsError = true;
            }
            // Kai9必須カラムが存在する事(更新日時)
            IsHitKai9 = false;
            for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
                Models Models = Kai9ComUtils.modelList.get(i);
                if (!Models.columnname.equals("update_date")) continue;
                IsHitKai9 = true;
                break;
            }
            if (!IsHitKai9) {
                Errors.add("【更新日時(update_date)】はKai9必須カラムなので省略できません。シート名=" + pWs.getSheetName() + RN);
                IsError = true;
            }
            // Kai9必須カラムが存在する事(削除フラグ)
            IsHitKai9 = false;
            for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
                Models Models = Kai9ComUtils.modelList.get(i);
                if (!Models.columnname.equals("delflg")) continue;
                IsHitKai9 = true;
                break;
            }
            if (!IsHitKai9) {
                Errors.add("【削除フラグ(delflg)】はKai9必須カラムなので省略できません。シート名=" + pWs.getSheetName() + RN);
                IsError = true;
            }
        }

        // Kai9必須カラムが存在する事(更新回数)
        IsHitKai9 = false;
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            // modify_count制御(親子関係がある場合、親と子で制御方法を変える)
            if (Models.columnname.equals("modify_count")) {
                if (Models.columnname.equals("modify_count")) {
                    IsHitKai9 = true;
                    break;
                } else {
                    String lastCharacter_ColumnName = Models.columnname.substring(Models.columnname.length() - 1);
                    if (lastCharacter_ColumnName.equals(lastCharacter_ClassName)) {
                        IsHitKai9 = true;
                        break;
                    }
                }
            }
        }
        if (!IsHitKai9) {
            Errors.add("【更新回数(modify_count)】はKai9必須カラムなので省略できません。シート名=" + pWs.getSheetName() + RN);
            IsError = true;
        }

        // byteaの場合、ファイルとして扱うので、「同名_filename」のカラムが別に存在している事をチェックする
        for (int i1 = 0; i1 < Kai9ComUtils.modelList.size(); i1++) {
            Models Models1 = Kai9ComUtils.modelList.get(i1);
            if (!Models1.data_type.equals("bytea")) continue;
            boolean hit_flg = false;

            for (int i2 = 0; i2 < Kai9ComUtils.modelList.size(); i2++) {
                Models Models2 = Kai9ComUtils.modelList.get(i2);
                if (Models2.columnname.equals(Models1.columnname)) continue;
                if (!(Models1.columnname + "_filename").equals(Models2.columnname)) continue;
                hit_flg = true;
            }
            if (!hit_flg) {
                Errors.add("【bytea型(" + Models1.columnname + ")】にはセットで「ファイル名を格納するカラム(" + Models1.columnname + "_filename" + ")」が必要です。シート名=" + pWs.getSheetName() + RN);
                IsError = true;
            }
        }

        if (IsError) {
            // 明細にある場合、エラー表示して抜ける
            Kai9Util.ErrorMsg(String.join("", Errors));
            return false;
        }

        // ======================================================================
        // .action.ts
        // ======================================================================
        // PK制御
        String IDs10 = "";
        String IDs11 = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (!Models.is_pk) continue;
            if (Models.columnname.equals("modify_count")) continue;
            if (IDs10.equals("")) {
                IDs10 = Models.columnname + ": " + CnvType(Models.data_type);
                IDs11 = Models.columnname + ": " + Models.columnname;
            } else {
                IDs10 = IDs10 + ", " + Models.columnname + ": " + CnvType(Models.data_type);
                IDs11 = IDs11 + ", " + Models.columnname + ": " + Models.columnname;
                ;
            }
        }
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\React\\store\\actions");
        // 雛形から複製
        Path sourcePath = Paths.get(Svn_Path + "\\src\\store\\actions\\" + src_classname + ".action.ts");
        Path targetPath = Paths.get(OurDir + "\\React\\store\\actions\\" + classname + ".action.ts");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        File file = new File(OurDir + "\\React\\store\\actions\\" + classname + ".action.ts");
        String content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        // 置換
        content = content.replace(src_classname, classname);
        content = content.replace(src_Classname, Classname);
        content = content.replace(src_CLASSNAME, CLASSNAME);
        content = content.replace("s_pk: number", IDs10);
        content = content.replace("s_pk: s_pk", IDs11);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // Pagenation.action.ts
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\React\\store\\actions");
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\src\\store\\actions\\" + src_classname + "Pagenation.action.ts");
        targetPath = Paths.get(OurDir + "\\React\\store\\actions\\" + classname + "Pagenation.action.ts");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(OurDir + "\\React\\store\\actions\\" + classname + "Pagenation.action.ts");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        // 置換
        content = content.replace(src_CLASSNAME, CLASSNAME);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // Models(.interface.ts)
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\React\\store\\Models");
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\src\\store\\Models\\" + src_classname + ".interface.ts");
        targetPath = Paths.get(OurDir + "\\React\\store\\Models\\" + classname + ".interface.ts");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(OurDir + "\\React\\store\\Models\\" + classname + ".interface.ts");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        // 置換
        content = content.replace(src_Classname, Classname);
        // 置換(カラム要素)
        String replaceStr = "";
        // 各カラム事にコードを生成
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            replaceStr += "    " + Models.columnname + ": " + CnvType(Models.data_type) + ";" + RN;
        }
        // 「[//制御:開始】対象カラム」から始まり、「【制御:終了】対象カラム」で終わる箇所と、生成したコードを置換する
        String startMarker = "【制御:開始】対象カラム";
        String endMarker = "【制御:終了】対象カラム";
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, startMarker, endMarker, isTargetStrLeave);

        // 【制御:開始】relation から始まり、【制御:終了】relation で終わる箇所を抽出
        startMarker = "【制御:開始】relation";
        endMarker = "【制御:終了】relation";
        List<Map.Entry<String, String>> replacements = new ArrayList<>();
        for (Relations Relations : Kai9ComUtils.RelationsList) {
            // テーブル名の箇所を置換
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", Relations.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", Relations.columnB));
        }
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // reducers(.reducer.ts)
        // ======================================================================
        // PK制御
        String IDs8 = "";
        String IDs9 = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (!Models.is_pk) continue;
            if (Models.columnname.equals("modify_count")) continue;
            if (IDs8.equals("")) {
                IDs8 = "pr." + Models.columnname + " === action." + classname + "." + Models.columnname;
                IDs9 = "pr." + Models.columnname + " !== action." + Models.columnname;
            } else {
                IDs8 = IDs8 + " && " + "pr." + Models.columnname + " === action." + classname + "." + Models.columnname;
                IDs9 = IDs9 + " && " + "pr." + Models.columnname + " !== action." + Models.columnname;
            }
        }
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\React\\store\\reducers");
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\src\\store\\reducers\\" + src_classname + ".reducer.ts");
        targetPath = Paths.get(OurDir + "\\React\\store\\reducers\\" + classname + ".reducer.ts");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(OurDir + "\\React\\store\\reducers\\" + classname + ".reducer.ts");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        // 置換
        content = content.replace("pr.s_pk === action.single_table.s_pk", IDs8);
        content = content.replace("pr.s_pk !== action.s_pk", IDs9);
        content = content.replace(src_classname, classname);
        content = content.replace(src_Classname, Classname);
        content = content.replace(src_CLASSNAME, CLASSNAME);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // reducers(Pagenation.reducer.ts)
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\React\\store\\reducers");
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\src\\store\\reducers\\" + src_classname + "Pagenation.reducer.ts");
        targetPath = Paths.get(OurDir + "\\React\\store\\reducers\\" + classname + "Pagenation.reducer.ts");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(OurDir + "\\React\\store\\reducers\\" + classname + "Pagenation.reducer.ts");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        // 置換
        content = content.replace(src_classname, classname);
        content = content.replace(src_Classname, Classname);
        content = content.replace(src_CLASSNAME, CLASSNAME);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // components(HistoryList.css)
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\React\\components\\" + Classname);
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\src\\components\\" + src_Classname + "\\" + src_Classname + "HistoryList.module.css");
        targetPath = Paths.get(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + "HistoryList.module.css");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        // ======================================================================
        // components(HistoryList.tsx)
        // ======================================================================
        // PK制御
        String IDs2 = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (!Models.is_pk) continue;
            if (Models.columnname.equals("modify_count")) continue;
            if (IDs2.equals("")) {
                IDs2 = classname + "_${" + classname + "." + Models.columnname + "}";
            } else {
                IDs2 = IDs2 + classname + "_${" + classname + "." + Models.columnname + "}";
            }
        }
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\React\\components\\" + Classname);
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\src\\components\\" + src_Classname + "\\" + src_Classname + "HistoryList.tsx");
        targetPath = Paths.get(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + "HistoryList.tsx");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + "HistoryList.tsx");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        // 置換 IDs2
        content = content.replace("`" + src_classname + "_${" + src_classname + ".s_pk}`", "`" + IDs2 + "`");
        // 置換(BLOBがある場合の処理を追加)
        if (is_bytea_exist) {
            // 置換(BLOBがある場合のImport追加)
            replaceStr = "";
            replaceStr += "import {API_URL} from \"../../common/constants\";" + RN;
            replaceStr += "import axios from 'axios';" + RN;
            replaceStr += "import Swal from 'sweetalert2';" + RN;
            if (isTargetStrLeave) {
                content = content.replace("//【制御:置換】BLOB有無①", "//【制御:置換】BLOB有無①" + replaceStr);
            } else {
                content = content.replace("//【制御:置換】BLOB有無①", replaceStr);
            }

            replaceStr = "";
            for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
                Models Models = Kai9ComUtils.modelList.get(i);
                if (!Models.data_type.equals("bytea")) continue;
                replaceStr += "const " + Models.columnname + "_ClickDownloadButton = async (" + classname + ": I" + Classname + ")  => {" + RN;
                replaceStr += "      const utl = API_URL+'/api/" + classname + "_" + Models.columnname + "_download';" + RN;
                replaceStr += "      const data = { " + RN;
                boolean isFirst = true;
                for (int i2 = 0; i2 < Kai9ComUtils.modelList.size(); i2++) {
                    Models Models2 = Kai9ComUtils.modelList.get(i2);
                    if (!Models2.is_pk) continue;
                    if (isFirst) {
                        isFirst = false;
                        replaceStr += "                     " + Models2.columnname + ": " + classname + "." + Models2.columnname;
                    } else {
                        replaceStr += "," + RN;
                        replaceStr += "                     " + Models2.columnname + ": " + classname + "." + Models2.columnname;
                    }
                }
                replaceStr += RN;
                replaceStr += "                   };" + RN;
                replaceStr += "      //「application/x-www-form-urlencoded」はURLエンコードした平文での送信" + RN;
                replaceStr += "      await axios.post(utl, data, {withCredentials: true, responseType: 'blob', headers: {'content-type': 'application/x-www-form-urlencoded'} })" + RN;
                replaceStr += "  " + RN;
                replaceStr += "      .then(function (response) {" + RN;
                replaceStr += "  " + RN;
                replaceStr += "        //ノーヒット時はnullが返るので抜ける" + RN;
                replaceStr += "        if (!response.data){return}" + RN;
                replaceStr += "  " + RN;
                replaceStr += "        //エラー発生時は抜ける" + RN;
                replaceStr += "        if (response.data.return_code && response.data.return_code!=0){" + RN;
                replaceStr += "          Swal.fire({" + RN;
                replaceStr += "            title: 'Error!'," + RN;
                replaceStr += "            text: response.data.msg," + RN;
                replaceStr += "            icon: 'error'," + RN;
                replaceStr += "            confirmButtonText: 'OK'" + RN;
                replaceStr += "          })" + RN;
                replaceStr += "          return;" + RN;
                replaceStr += "        }" + RN;
                replaceStr += "        " + RN;
                replaceStr += "        const blob = new Blob([response.data], { type: \"application/octet-stream\" });" + RN;
                replaceStr += "        const url = window.URL.createObjectURL(blob);" + RN;
                replaceStr += "        const a = document.createElement(\"a\");" + RN;
                replaceStr += "        a.href = url;" + RN;
                replaceStr += "        a.download = " + classname + "." + Models.columnname + "_filename as string;" + RN;
                replaceStr += "        a.click();" + RN;
                replaceStr += "        a.remove();" + RN;
                replaceStr += "        URL.revokeObjectURL(url); " + RN;
                replaceStr += "" + RN;
                replaceStr += "      })" + RN;
                replaceStr += "      .catch(function (error) {" + RN;
                replaceStr += "        // 送信失敗時" + RN;
                replaceStr += "        Swal.fire({" + RN;
                replaceStr += "          title: 'Error!'," + RN;
                replaceStr += "          text: error.message," + RN;
                replaceStr += "          icon: 'error'," + RN;
                replaceStr += "          confirmButtonText: 'OK'" + RN;
                replaceStr += "        })" + RN;
                replaceStr += "      });" + RN;
                replaceStr += "    };" + RN;
                replaceStr += "" + RN;
            }
            if (isTargetStrLeave) {
                content = content.replace("//【制御:置換】BLOB有無②", "//【制御:置換】BLOB有無②" + replaceStr);
            } else {
                content = content.replace("//【制御:置換】BLOB有無②", replaceStr);
            }

        } else {
            // BLOBが無い場合に制御文字を消す
            if (!isTargetStrLeave) {
                content = content.replaceAll(Pattern.quote("//【制御:置換】BLOB有無①") + "\\s*\\n?", "");
                content = content.replaceAll(Pattern.quote("//【制御:置換】BLOB有無②") + "\\s*\\n?", "");
            }
        }

        // 置換(カラム要素) 制御:開始】対象カラム①
        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.columnname.equals("modify_count")) continue;
            if (!Models.special_control_relation.isEmpty()) {
                Relations RelationsTmp = parse_Relations(Models.special_control_relation, Models.columnname);
                replaceStr += "        <td>{" + classname + "." + RelationsTmp.columnA + "__" + RelationsTmp.columnB + "}</td>" + RN;
            } else if (CnvType(Models.data_type).equals("boolean")) {
                replaceStr += "        <td>{" + classname + "." + Models.columnname + "? \"〇\":\"\"}</td>" + RN;
            } else if (CnvType(Models.data_type).equals("Date")) {
                replaceStr += "        <td>{moment(" + classname + "." + Models.columnname + ").format('YYYY/MM/DD HH:mm:ss')}</td>" + RN;
            } else if (Models.data_type.equals("bytea")) {
                replaceStr += "        <td><button className=\"btn btn-outline-dark\" onClick={() => { " + Models.columnname + "_ClickDownloadButton(" + classname + "); }}>DownLoad</button></td>" + RN;
            } else {
                replaceStr += "        <td>{" + classname + "." + Models.columnname + "}</td>" + RN;
            }
        }
        // 末尾の改行をカット
        if (replaceStr.endsWith("\r\n")) {
            replaceStr = replaceStr.substring(0, replaceStr.length() - 2);
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】対象カラム①", "【制御:終了】対象カラム①", isTargetStrLeave);

        // 置換(カラム要素) 制御:開始】対象カラム②
        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.columnname.equals("modify_count")) continue;
            replaceStr += "                      <th scope=\"col\">" + Models.FieldName_J + "</th>" + RN;
        }
        // 末尾の改行をカット
        if (replaceStr.endsWith("\r\n")) {
            replaceStr = replaceStr.substring(0, replaceStr.length() - 2);
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】対象カラム②", "【制御:終了】対象カラム②", isTargetStrLeave);

        // 置換
        content = content.replace(src_classname, classname);
        content = content.replace(src_Classname, Classname);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // components(.module.css)
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\React\\components\\" + Classname);
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\src\\components\\" + src_Classname + "\\" + src_classname + ".module.css");
        targetPath = Paths.get(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + ".module.css");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + ".module.css");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        // 置換
        content = content.replace(src_classname, classname);
        content = content.replace(src_Classname, Classname);
        content = content.replace(src_CLASSNAME, CLASSNAME);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // components(List.module.css)
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\React\\components\\" + Classname);
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\src\\components\\" + src_Classname + "\\" + src_Classname + "List.module.css");
        targetPath = Paths.get(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + "List.module.css");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        // PK制御
        String IDs1 = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (!Models.is_pk) continue;
            if (Models.columnname.equals("modify_count")) continue;
            if (IDs1.equals("")) {
                IDs1 = classname + "s.selected" + Classname + "." + Models.columnname + " === " + classname + "." + Models.columnname + "";
            } else {
                IDs1 = IDs1 + " && " + classname + "s.selected" + Classname + "." + Models.columnname + " === " + classname + "." + Models.columnname + "";
            }
        }
        String IDs3 = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (!Models.is_pk) continue;
            if (Models.columnname.equals("modify_count")) continue;
            if (IDs3.equals("")) {
                IDs3 = "{" + classname + "." + Models.columnname + "}";
            } else {
                IDs3 = IDs3 + "\"_\"" + "{" + classname + "." + Models.columnname + "}";
            }
        }
        // ======================================================================
        // components(List.tsx)
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\React\\components\\" + Classname);
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\src\\components\\" + src_Classname + "\\" + src_Classname + "List.tsx");
        targetPath = Paths.get(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + "List.tsx");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + "List.tsx");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        // 置換 IDs1
        content = content.replace(src_classname + "s.selected" + src_Classname + ".s_pk === " + src_classname + ".s_pk", IDs1);
        // 置換 IDs2
        content = content.replace("`" + src_classname + "_${" + src_classname + ".s_pk}`", "`" + IDs2 + "`");
        // 置換 IDs3
        content = content.replace("<th scope=\"row\">{single_table.s_pk}</th>", "<th scope=\"row\">" + IDs3 + "</th>");
        // 置換(BLOBがある場合の処理を追加)
        if (is_bytea_exist) {
            // 置換(BLOBがある場合のImport追加)
            replaceStr = "";
            replaceStr += "import {API_URL} from \"../../common/constants\";" + RN;
            replaceStr += "import axios from 'axios';" + RN;
            replaceStr += "import Swal from 'sweetalert2';" + RN;
            if (isTargetStrLeave) {
                content = content.replace("//【制御:置換】BLOB有無①", "//【制御:置換】BLOB有無①" + replaceStr);
            } else {
                content = content.replace("//【制御:置換】BLOB有無①", replaceStr);
            }

            replaceStr = "";
            for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
                Models Models = Kai9ComUtils.modelList.get(i);
                if (!Models.data_type.equals("bytea")) continue;
                replaceStr += "const " + Models.columnname + "_ClickDownloadButton = async (" + classname + ": I" + Classname + ")  => {" + RN;
                replaceStr += "      const utl = API_URL+'/api/" + classname + "_" + Models.columnname + "_download';" + RN;
                replaceStr += "      const data = { " + RN;
                boolean isFirst = true;
                for (int i2 = 0; i2 < Kai9ComUtils.modelList.size(); i2++) {
                    Models Models2 = Kai9ComUtils.modelList.get(i2);
                    if (!Models2.is_pk) continue;
                    if (isFirst) {
                        isFirst = false;
                        replaceStr += "                     " + Models2.columnname + ": " + classname + "." + Models2.columnname;
                    } else {
                        replaceStr += "," + RN;
                        replaceStr += "                     " + Models2.columnname + ": " + classname + "." + Models2.columnname;
                    }
                }
                replaceStr += RN;
                replaceStr += "                   };" + RN;
                replaceStr += "      //「application/x-www-form-urlencoded」はURLエンコードした平文での送信" + RN;
                replaceStr += "      await axios.post(utl, data, {withCredentials: true, responseType: 'blob', headers: {'content-type': 'application/x-www-form-urlencoded'} })" + RN;
                replaceStr += "  " + RN;
                replaceStr += "      .then(function (response) {" + RN;
                replaceStr += "  " + RN;
                replaceStr += "        //ノーヒット時はnullが返るので抜ける" + RN;
                replaceStr += "        if (!response.data){return}" + RN;
                replaceStr += "  " + RN;
                replaceStr += "        //エラー発生時は抜ける" + RN;
                replaceStr += "        if (response.data.return_code && response.data.return_code!=0){" + RN;
                replaceStr += "          Swal.fire({" + RN;
                replaceStr += "            title: 'Error!'," + RN;
                replaceStr += "            text: response.data.msg," + RN;
                replaceStr += "            icon: 'error'," + RN;
                replaceStr += "            confirmButtonText: 'OK'" + RN;
                replaceStr += "          })" + RN;
                replaceStr += "          return;" + RN;
                replaceStr += "        }" + RN;
                replaceStr += "        " + RN;
                replaceStr += "        const blob = new Blob([response.data], { type: \"application/octet-stream\" });" + RN;
                replaceStr += "        const url = window.URL.createObjectURL(blob);" + RN;
                replaceStr += "        const a = document.createElement(\"a\");" + RN;
                replaceStr += "        a.href = url;" + RN;
                replaceStr += "        a.download = " + classname + "." + Models.columnname + "_filename as string;" + RN;
                replaceStr += "        a.click();" + RN;
                replaceStr += "        a.remove();" + RN;
                replaceStr += "        URL.revokeObjectURL(url); " + RN;
                replaceStr += "" + RN;
                replaceStr += "      })" + RN;
                replaceStr += "      .catch(function (error) {" + RN;
                replaceStr += "        // 送信失敗時" + RN;
                replaceStr += "        Swal.fire({" + RN;
                replaceStr += "          title: 'Error!'," + RN;
                replaceStr += "          text: error.message," + RN;
                replaceStr += "          icon: 'error'," + RN;
                replaceStr += "          confirmButtonText: 'OK'" + RN;
                replaceStr += "        })" + RN;
                replaceStr += "      });" + RN;
                replaceStr += "    };" + RN;
                replaceStr += "" + RN;
            }
            if (isTargetStrLeave) {
                content = content.replace("//【制御:置換】BLOB有無②", "//【制御:置換】BLOB有無②" + replaceStr);
            } else {
                content = content.replace("//【制御:置換】BLOB有無②", replaceStr);
            }

        } else {
            if (!isTargetStrLeave) {
                content = content.replaceAll(Pattern.quote("//【制御:置換】BLOB有無①") + "\\s*\\n?", "");
                content = content.replaceAll(Pattern.quote("//【制御:置換】BLOB有無②") + "\\s*\\n?", "");
            }
        }

        // 置換(カラム要素) 制御:開始】対象カラム①
        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.columnname.equals("modify_count")) continue;
            if (Models.columnname.equals("update_u_id")) {
                // 更新者はIDではなく名称で表示
                replaceStr += "        <td>{" + classname + ".update_user}</td>" + RN;
                continue;
            }
            if (!Models.special_control_relation.isEmpty()) {
                Relations RelationsTmp = parse_Relations(Models.special_control_relation, Models.columnname);
                replaceStr += "        <td>{" + classname + "." + RelationsTmp.columnA + "__" + RelationsTmp.columnB + "}</td>" + RN;
            } else if (CnvType(Models.data_type).equals("boolean")) {
                replaceStr += "        <td>{" + classname + "." + Models.columnname + "? \"〇\":\"\"}</td>" + RN;
            } else if (Models.data_type.equals("timestamp")) {
                replaceStr += "        <td>{moment(" + classname + "." + Models.columnname + ").format('YYYY/MM/DD HH:mm:ss')}</td>" + RN;
            } else if (Models.data_type.equals("date")) {
                replaceStr += "        <td>{moment(" + classname + "." + Models.columnname + ").format('YYYY/MM/DD')}</td>" + RN;
            } else if (Models.data_type.equals("time")) {
                replaceStr += "        <td>{moment(" + classname + "." + Models.columnname + ").format('HH:mm:ss')}</td>" + RN;
            } else if (Models.data_type.equals("bytea")) {
                replaceStr += "        <td><button className=\"btn btn-outline-dark\" onClick={() => { " + Models.columnname + "_ClickDownloadButton(" + classname + "); }}>DownLoad</button></td>" + RN;
            } else {
                replaceStr += "        <td>{" + classname + "." + Models.columnname + "}</td>" + RN;
            }
        }
        // 末尾の改行をカット
        if (replaceStr.endsWith("\r\n")) {
            replaceStr = replaceStr.substring(0, replaceStr.length() - 2);
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】対象カラム①", "【制御:終了】対象カラム①", isTargetStrLeave);

        // 置換(カラム要素) 制御:開始】対象カラム②
        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.columnname.equals("modify_count")) continue;
            replaceStr += "            <th scope=\"col\">" + Models.FieldName_J + "</th>" + RN;
        }
        // 末尾の改行をカット
        if (replaceStr.endsWith("\r\n")) {
            replaceStr = replaceStr.substring(0, replaceStr.length() - 2);
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】対象カラム②", "【制御:終了】対象カラム②", isTargetStrLeave);

        // 置換
        content = content.replace(src_classname, classname);
        content = content.replace(src_Classname, Classname);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // components(.tsx) ※Single_table.tsx等
        // ======================================================================
        // PK制御
        String IDs4 = "";
        String IDs5 = "";
        String IDs12 = "";
        String IDs13 = "";
        String IDs14 = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (!Models.is_pk) continue;
            if (Models.columnname.equals("modify_count")) continue;
            if (IDs4.equals("")) {
                IDs4 = Models.FieldName_J + "=${" + classname + "s.selected" + Classname + "." + Models.columnname + "}";
                IDs5 = classname + "s.selected" + Classname + "." + Models.columnname;
                IDs12 = Models.columnname + ": " + Models.columnname;
                IDs13 = Models.columnname + ":" + CnvType(Models.data_type);
                IDs14 = classname + "s.selected" + Classname + "." + Models.columnname;
            } else {
                IDs4 = IDs4 + "," + Models.FieldName_J + "=${" + classname + "s.selected" + Classname + "." + Models.columnname + "}";
                IDs5 = IDs5 + "," + classname + "s.selected" + Classname + "." + Models.columnname;
                IDs12 = IDs12 + "," + Models.columnname + ": " + Models.columnname;
                IDs13 = IDs13 + "," + Models.columnname + ":" + CnvType(Models.data_type);
                IDs14 = IDs14 + "," + classname + "s.selected" + Classname + "." + Models.columnname;
            }
        }
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\React\\components\\" + Classname);
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\src\\components\\" + src_Classname + "\\" + src_classname + ".tsx");
        targetPath = Paths.get(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + ".tsx");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + ".tsx");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        // 置換 IDs4
        content = content.replace("シングルID=${single_tables.selectedSingle_table.s_pk}", IDs4);
        // 置換 IDs5
        content = content.replace("removeSingle_table(single_tables.selectedSingle_table.s_pk", "removeSingle_table(" + IDs5);
        // 置換 IDs12
        content = content.replace("const data = {s_pk: s_pk", "const data = {" + IDs12);
        // 置換 IDs13
        content = content.replace("FindSingle_tableHistory(s_pk:number", "FindSingle_tableHistory(" + IDs13);
        // 置換 IDs14
        content = content.replace("FindSingle_tableHistory(single_tables.selectedSingle_table.s_pk", "FindSingle_tableHistory(" + IDs14);

        // 【制御:開始】relation① から始まり、【制御:終了】relation① で終わる箇所を抽出
        startMarker = "【制御:開始】relation①";
        endMarker = "【制御:終了】relation①";
        replacements = new ArrayList<>();
        for (Relations Relations : Kai9ComUtils.RelationsList) {
            // テーブル名の末尾から「_a」又は「_b」を取り除く
            String table_name = Relations.tableA;
            if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                table_name = table_name.substring(0, table_name.length() - 2);
            }

            // 必要箇所を置換
            replacements.add(new AbstractMap.SimpleEntry<>("related_table", table_name));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", Relations.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", Relations.columnB));
        }
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // 【制御:開始】relation② から始まり、【制御:終了】relation② で終わる箇所を抽出
        startMarker = "【制御:開始】relation②";
        endMarker = "【制御:終了】relation②";
        replacements = new ArrayList<>();
        for (Relations Relations : Kai9ComUtils.RelationsList) {
            // テーブル名の末尾から「_a」又は「_b」を取り除く
            String table_name = Relations.tableA;
            if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                table_name = table_name.substring(0, table_name.length() - 2);
            }

            // 必要箇所を置換
            replacements.add(new AbstractMap.SimpleEntry<>("related_table", table_name));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", Relations.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", Relations.columnB));
        }
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // 【制御:開始】relation③ から始まり、【制御:終了】relation③ で終わる箇所を抽出
        startMarker = "【制御:開始】relation③";
        endMarker = "【制御:終了】relation③";
        replacements = new ArrayList<>();
        for (Relations Relations : Kai9ComUtils.RelationsList) {
            // テーブル名の末尾から「_a」又は「_b」を取り除く
            String table_name = Relations.tableA;
            if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                table_name = table_name.substring(0, table_name.length() - 2);
            }

            // 必要箇所を置換
            replacements.add(new AbstractMap.SimpleEntry<>("related_table", table_name));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", Relations.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", Relations.columnB));
        }
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        startMarker = "【制御:開始】relation④";
        endMarker = "【制御:終了】relation④";
        replacements = new ArrayList<>();
        for (Relations Relations : Kai9ComUtils.RelationsList) {
            // 必要箇所を置換
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a.related_pk", Relations.tableA + "." + Relations.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a.related_data", Relations.tableB + "." + Relations.columnB));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", Relations.src_column));
        }
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);
        
        startMarker = "【制御:開始】relation⑤";
        endMarker = "【制御:終了】relation⑤";
        replacements = new ArrayList<>();
        for (Relations Relations : Kai9ComUtils.RelationsList) {
            // 必要箇所を置換
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a.related_pk", Relations.tableA + "." + Relations.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a.related_data", Relations.tableB + "." + Relations.columnB));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", Relations.src_column));
        }
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);
        
        startMarker = "【制御:開始】relation⑥";
        endMarker = "【制御:終了】relation⑥";
        replacements = new ArrayList<>();
        for (Relations Relations : Kai9ComUtils.RelationsList) {
            // 必要箇所を置換
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a.related_pk", Relations.tableA + "." + Relations.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_table_a.related_data", Relations.tableB + "." + Relations.columnB));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", Relations.src_column));
        }
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);
        
        // 正規表現チェック
        // 雛形コード取り出し
        String startTag = "{/* 【制御:開始】正規表現チェック */}";
        String endTag = "{/* 【制御:終了】正規表現チェック */}";
        String[] results = extractContent(content, startTag, endTag, isTargetStrLeave);
        String Line_TextInput = results[0];
        content = results[1];

        String insertTag = "{/* 【制御:挿入行]正規表現チェック */}";
        String tmpStr = "";
        String orignPatternRegexp = "pattern = /^[^\\x20-\\x7E｡-ﾟ]*$/";
        String orignPatternMessage = "errorMessage = '全角で入力して下さい'";
        boolean is_target_exist = false;
        for (int i = Kai9ComUtils.modelList.size() - 1; i >= 0; i--) {
            Models Models = Kai9ComUtils.modelList.get(i);
            // PKは無視
            if (Models.is_pk) continue;
            // システムカラムも無視
            if (Models.columnname.equals("modify_count")) continue;
            if (Models.columnname.equals("update_u_id")) continue;
            if (Models.columnname.equals("update_date")) continue;

            // ※注意事項：「全角限定」を元ネタとして利用。置換するので、元ネタの置換前コードが書き換えられてると上手く動作しないので注意。
            tmpStr = Line_TextInput;
            tmpStr = tmpStr.replace("fullwidth_limited", Models.columnname);
            tmpStr = tmpStr.replace("全角限定", Models.FieldName_J);

            if ((Models.input_type.toUpperCase().equals("TEXTINPUT") || Models.input_type.toUpperCase().equals("TEXTAREA")) && !Models.validation_check.isEmpty()) {
                boolean is_making = true;
                // バリデーションチェック
                if (Models.validation_check.equals("全角限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "pattern = /^[^\\x20-\\x7E｡-ﾟ]*$/");
                    tmpStr = tmpStr.replace(orignPatternMessage, "errorMessage = '全角で入力して下さい'");
                } else if (Models.validation_check.equals("半角限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "pattern = /^[\\x20-\\x7E｡-ﾟ]*$/");
                    tmpStr = tmpStr.replace(orignPatternMessage, "errorMessage = '半角で入力して下さい'");
                } else if (Models.validation_check.equals("半角英字限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "pattern = /^[a-zA-Z]*$/");
                    tmpStr = tmpStr.replace(orignPatternMessage, "errorMessage = '半角英字で入力して下さい'");
                } else if (Models.validation_check.equals("半角数字限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "pattern = /^[0-9]*$/");
                    tmpStr = tmpStr.replace(orignPatternMessage, "errorMessage = '半角数字で入力して下さい'");
                } else if (Models.validation_check.equals("半角記号限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "pattern = /^[!\"#$%&'()*+,\\-./:;<=>?@[\\\\\\]^_`{|}~]*$/");
                    tmpStr = tmpStr.replace(orignPatternMessage, "errorMessage = '半角記号で入力して下さい'");
                } else if (Models.validation_check.equals("半角カナ限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "pattern = /^[ｦ-ﾝﾞﾟ]*$/");
                    tmpStr = tmpStr.replace(orignPatternMessage, "errorMessage = '半角カナで入力して下さい'");
                } else if (Models.validation_check.equals("全角カナ限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "pattern = /^[ァ-ヶ]*$/");
                    tmpStr = tmpStr.replace(orignPatternMessage, "errorMessage = '全角カナで入力して下さい'");
                } else if (Models.validation_check.equals("郵便番号")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "pattern = /^\\d{3}-?\\d{4}$/");
                    tmpStr = tmpStr.replace(orignPatternMessage, "errorMessage = '郵便番号の入力形式が不正です'");
                } else if (Models.validation_check.equals("電話番号")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "pattern = /^\\d{2,4}-?\\d{2,4}-?\\d{3,4}$/");
                    tmpStr = tmpStr.replace(orignPatternMessage, "errorMessage = '電話番号の入力形式が不正です'");
                } else if (Models.validation_check.equals("メールアドレス")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "pattern = /^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$/");
                    tmpStr = tmpStr.replace(orignPatternMessage, "errorMessage = 'メールアドレスの形式が不正です'");
                } else if (Models.validation_check.equals("URL")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "pattern = /^(https?|ftp):\\/\\/[^\\s/$.?#].[^\\s]*$/");
                    tmpStr = tmpStr.replace(orignPatternMessage, "errorMessage = 'URLの入力形式が不正です'");
                } else {
                    is_making = false;
                }
                if (Models.validation_check.contains("【正規表現】")) {
                    String tmpregexp = Models.validation_check.replace("【正規表現】", "");
                    tmpStr = tmpStr.replace(orignPatternRegexp, "pattern = /" + tmpregexp + "/");
                    tmpStr = tmpStr.replace(orignPatternMessage, "errorMessage = '入力形式が不正です'");
                    is_making = true;
                }
                if (is_making) {
                    content = content.replace(insertTag, insertTag + RN + tmpStr + RN);
                    is_target_exist = true;
                }
            }

        }
        if (!is_target_exist && Kai9ComUtils.RelationsList.isEmpty()) {
            // 正規表現とコードチェック(RelationsListが空か否かで判定)で、ターゲットが全くなかった場合、型宣言をしている箇所のコードも削除する
            startTag = "{/* 【制御:開始】共通型宣言 */}";
            endTag = "{/* 【制御:終了】共通型宣言 */}";
            results = extractContent(content, startTag, endTag, isTargetStrLeave);
            Line_TextInput = results[0];
            content = results[1];
        }
        if (!is_target_exist) {
            // 正規表現で、ターゲットが全くなかった場合、型宣言をしている箇所のコードも削除する
            startTag = "{/* 【制御:開始】正規表現型宣言 */}";
            endTag = "{/* 【制御:終了】正規表現型宣言 */}";
            results = extractContent(content, startTag, endTag, isTargetStrLeave);
            Line_TextInput = results[0];
            content = results[1];
        }
        if (Kai9ComUtils.RelationsList.isEmpty()) {
            // コードチェック(RelationsListが空か否かで判定)で、ターゲットが全くなかった場合、型宣言をしている箇所のコードも削除する
            startTag = "{/* 【制御:開始】コード値型宣言 */}";
            endTag = "{/* 【制御:終了】コード値型宣言 */}";
            results = extractContent(content, startTag, endTag, isTargetStrLeave);
            Line_TextInput = results[0];
            content = results[1];
        }

        if (!isTargetStrLeave) {
            String Strtag = "{/* 【制御:開始】共通型宣言 */}";
            // \\Q と \\E は、正規表現の中でリテラル文字列として扱うためのエスケープシーケンス
            content = content.replaceAll(".*" + "\\Q" + Strtag + "\\E" + ".*\\R?", "");
            Strtag = "{/* 【制御:終了】共通型宣言 */}";
            content = content.replaceAll(".*" + "\\Q" + Strtag + "\\E" + ".*\\R?", "");
            Strtag = "{/* 【制御:開始】正規表現型宣言 */}";
            content = content.replaceAll(".*" + "\\Q" + Strtag + "\\E" + ".*\\R?", "");
            Strtag = "{/* 【制御:終了】正規表現型宣言 */}";
            content = content.replaceAll(".*" + "\\Q" + Strtag + "\\E" + ".*\\R?", "");
            Strtag = "{/* 【制御:開始】コード値型宣言 */}";
            content = content.replaceAll(".*" + "\\Q" + Strtag + "\\E" + ".*\\R?", "");
            Strtag = "{/* 【制御:終了】コード値型宣言 */}";
            content = content.replaceAll(".*" + "\\Q" + Strtag + "\\E" + ".*\\R?", "");

            Strtag = "{/* 【制御:挿入行]正規表現チェック */}";
            content = content.replaceAll(".*" + "\\Q" + Strtag + "\\E" + ".*\\R?", "");
        }

        // 置換
        content = content.replace(src_classname, classname);
        content = content.replace(src_Classname, Classname);
        content = content.replace("シングル表", lTable_Name_J);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // components(Form.module.css)
        // ======================================================================
        // 出力先フォルダ作成
        Kai9Util.MakeDirs(OurDir + "\\React\\components\\" + Classname);
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\src\\components\\" + src_Classname + "\\" + src_classname + "Form.module.css");
        targetPath = Paths.get(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + "Form.module.css");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + "Form.module.css");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);
        // 置換
        content = content.replace(src_classname, classname);
        content = content.replace(src_Classname, Classname);
        content = content.replace(src_CLASSNAME, CLASSNAME);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // components(Form.tsx)
        // ======================================================================
        String IDs15 = "";
        String IDs16 = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (!Models.is_pk) continue;
            if (Models.columnname.equals("modify_count")) continue;
            if (IDs15.equals("")) {
                IDs15 = Models.FieldName_J.toUpperCase() + "=${formState." + Models.columnname + ".value}";
                IDs16 = "\"" + Models.FieldName_J + ":\"+formState." + Models.columnname + ".value";
            } else {
                IDs15 = IDs15 + "," + Models.FieldName_J.toUpperCase() + "=${formState." + Models.columnname + ".value}";
                IDs16 = IDs16 + "、\"" + Models.FieldName_J + ":\"+formState." + Models.columnname + ".value";
            }
        }

        Kai9Util.MakeDirs(OurDir + "\\React\\components\\" + Classname);
        // 雛形から複製
        sourcePath = Paths.get(Svn_Path + "\\src\\components\\" + src_Classname + "\\" + src_classname + "Form.tsx");
        targetPath = Paths.get(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + "Form.tsx");
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        // ファイル内容を UTF-8 エンコーディングで読み取り
        file = new File(OurDir + "\\React\\components\\" + Classname + "\\" + Classname + "Form.tsx");
        content = new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8);

        // 置換 IDs15
        content = content.replace("シングルID=${formState.s_pk.value}", IDs15);
        // 置換 IDs16
        content = content.replace("\"シングルID:\"+formState.s_pk.value", IDs16);

        // 特殊制御【relation】が無い場合はimport SelectInputを消す
        if (Kai9ComUtils.RelationsList.isEmpty()) {
            String SelectInput = "import SelectInput from \"../../common/components/Select\";" + "\r\n";
            content = content.replace(SelectInput, "");
        }

        // 特殊制御【relation】が無い場合はconst [loading, setLoading] = useState(true);を消す
        if (Kai9ComUtils.RelationsList.isEmpty()) {
            String SelectInput = "const [loading, setLoading] = useState(true);" + "\r\n";
            content = content.replace(SelectInput, "");
        }

        // const [SelectInput_related_table_related_pk_related_data_s, setSelectInput_related_table_related_pk_related_data_s] = useState([""]); の制御
        // 【制御:開始】relation① から始まり、【制御:終了】relation① で終わる箇所を抽出
        startMarker = "【制御:開始】relation①";
        endMarker = "【制御:終了】relation①";
        replacements = new ArrayList<>();
        for (Relations Relations : Kai9ComUtils.RelationsList) {
            // テーブル名の末尾から「_a」又は「_b」を取り除く
            String table_name = Relations.tableA;
            if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                table_name = table_name.substring(0, table_name.length() - 2);
            }

            // 必要箇所を置換
            replacements.add(new AbstractMap.SimpleEntry<>("related_table", table_name));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", Relations.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", Relations.columnB));
        }
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // 【制御:開始】relation② から始まり、【制御:終了】relation② で終わる箇所を抽出
        startMarker = "【制御:開始】relation②";
        endMarker = "【制御:終了】relation②";
        replacements = new ArrayList<>();
        for (Relations Relations : Kai9ComUtils.RelationsList) {
            // テーブル名の末尾から「_a」又は「_b」を取り除く
            String table_name = Relations.tableA;
            if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                table_name = table_name.substring(0, table_name.length() - 2);
            }

            // 必要箇所を置換(元カラム名を反映させる箇所)
            replacements.add(new AbstractMap.SimpleEntry<>("function get_related_pk_value(related_pk: any): string {", "function get_" + Relations.src_column + "_value(" + Relations.src_column + ": any): string {"));
            replacements.add(new AbstractMap.SimpleEntry<>("if (typeof related_pk == 'string' && related_pk.includes(':')) {", "if (typeof " + Relations.src_column + " == 'string' && " + Relations.src_column + ".includes(':')) {"));
            replacements.add(new AbstractMap.SimpleEntry<>("return related_pk;", "return " + Relations.src_column + ";"));
            replacements.add(new AbstractMap.SimpleEntry<>("if (related_pk == value) {", " if (" + Relations.src_column + " == value) {"));
            // 必要箇所を置換(他箇所)
            replacements.add(new AbstractMap.SimpleEntry<>("related_table", table_name));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", Relations.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", Relations.columnB));
        }
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // 【制御:開始】relation③ から始まり、【制御:終了】relation③ で終わる箇所を抽出
        startMarker = "【制御:開始】relation③";
        endMarker = "【制御:終了】relation③";
        replacements = new ArrayList<>();
        for (Relations Relations : Kai9ComUtils.RelationsList) {
            // テーブル名の末尾から「_a」又は「_b」を取り除く
            String table_name = Relations.tableA;
            if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                table_name = table_name.substring(0, table_name.length() - 2);
            }

            // 必要箇所を置換
            replacements.add(new AbstractMap.SimpleEntry<>("related_table", table_name));
            replacements.add(new AbstractMap.SimpleEntry<>("related_pk", Relations.columnA));
            replacements.add(new AbstractMap.SimpleEntry<>("related_data", Relations.columnB));
        }
        content = Kai9ComUtils.replaceContent(content, replacements, startMarker, endMarker, isTargetStrLeave);

        // 置換(カラム要素) 制御:開始】対象カラム①
        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (CnvType(Models.data_type).equals("string")) {
                replaceStr += "            " + Models.columnname + ":\"\"";
            } else if (CnvType(Models.data_type).equals("number")) {
                replaceStr += "            " + Models.columnname + ":0";
            } else if (CnvType(Models.data_type).equals("boolean")) {
                replaceStr += "            " + Models.columnname + ":false";
            } else if (CnvType(Models.data_type).equals("Date")) {
                replaceStr += "            " + Models.columnname + ":new Date";
            } else if (CnvType(Models.data_type).equals("Blob")) {
                replaceStr += "            " + Models.columnname + ":new Blob([\"\"], {type : 'application/json'})";
            } else {
                replaceStr += "            " + Models.columnname + ":初期値(未実装)";
            }
            // 行末のカンマ制御
            replaceStr += "," + RN;
        }
        // 更新者(非DB項目)を追加
        replaceStr += "            update_user:\"\",//非DB項目";
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】対象カラム①", "【制御:終了】対象カラム①", isTargetStrLeave);

        // 置換(カラム要素) 制御:開始】対象カラム②
        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            replaceStr += "    " + Models.columnname + ": { error: \"\", value: " + classname + "." + Models.columnname + " }," + RN;
        }
        // 更新者(非DB項目)を追加
        replaceStr += "    update_user: { error: \"\", value: " + classname + ".update_user },//非DB項目";
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】対象カラム②", "【制御:終了】対象カラム②", isTargetStrLeave);

        // 置換(カラム要素) 制御:開始】対象カラム③
        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.special_control_relation.isEmpty()) {
                if (Models.validation_check.equals("郵便番号")) {
                    // バリデーションチェックが郵便番号の場合、-を取り除くコードにする
                    replaceStr += "      " + Models.columnname + ": formState." + Models.columnname + ".value.replace(/-/g, ''),//-を取り除く" + RN;
                } else if (Models.validation_check.equals("日付") || Models.validation_check.equals("日時")) {
                    // バリデーションチェックが日付か日時の場合、API連携時のタイムゾーン誤差を防ぐため、UTCからローカルタイムゾーンへの変換を行う
                    replaceStr += "      " + Models.columnname + ": moment(formState." + Models.columnname + ".value).toDate(),//API連携時のタイムゾーン誤差を防ぐため、UTCからローカルタイムゾーンへの変換を行う" + RN;
                } else {
                    replaceStr += "      " + Models.columnname + ": formState." + Models.columnname + ".value," + RN;
                }
            } else {
                // 特殊制御(relation)
                if (CnvType(Models.data_type).equals("number")) {
                    replaceStr += "      " + Models.columnname + ": ConvValueNum(formState." + Models.columnname + ".value.toString())," + RN;
                } else {
                    replaceStr += "      " + Models.columnname + ": ConvValueStr(formState." + Models.columnname + ".value.toString())," + RN;
                }
            }
        }
        // 更新者(非DB項目)を追加
        replaceStr += "      update_user: ConvValueStr(formState.update_user.value.toString()),//非DB項目";
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】対象カラム③", "【制御:終了】対象カラム③", isTargetStrLeave);

        // 置換(カラム要素) 制御:開始】対象カラム④
        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (!Models.is_pk) continue;
            if (CnvType(Models.data_type).equals("number")) {
                replaceStr += "        formState." + Models.columnname + ".value = Number(response.data." + Models.columnname + ");" + RN;
            } else {
                replaceStr += "        formState." + Models.columnname + ".value = response.data." + Models.columnname + ";" + RN;
            }
        }
        // 末尾の改行をカット
        if (replaceStr.endsWith("\r\n")) {
            replaceStr = replaceStr.substring(0, replaceStr.length() - 2);
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】対象カラム④", "【制御:終了】対象カラム④", isTargetStrLeave);

        // 置換(カラム要素) 制御:開始】対象カラム⑤
        replaceStr = "";
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.special_control_relation.isEmpty()) {
                replaceStr += "        " + Models.columnname + ": formState." + Models.columnname + ".value," + RN;
            } else {
                // 特殊制御(relation)
                if (CnvType(Models.data_type).equals("number")) {
                    replaceStr += "        " + Models.columnname + ": ConvValueNum(formState." + Models.columnname + ".value.toString())," + RN;
                } else {
                    replaceStr += "        " + Models.columnname + ": ConvValueStr(formState." + Models.columnname + ".value.toString())," + RN;
                }
            }
        }
        // 更新者(非DB項目)を追加
        replaceStr += "        update_user: ConvValueStr(formState.update_user.value.toString()),//非DB項目";
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】対象カラム⑤", "【制御:終了】対象カラム⑤", isTargetStrLeave);

        // 置換(カラム要素) 制御:開始】対象カラム⑥
        replaceStr = "";
        boolean is_first = true;
        for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
            Models Models = Kai9ComUtils.modelList.get(i);
            if (is_first) {
                replaceStr += "           formState." + Models.columnname + ".error " + RN;
                is_first = false;
            } else {
                replaceStr += "        || formState." + Models.columnname + ".error" + RN;
            }
        }
        // 末尾の改行をカット
        if (replaceStr.endsWith("\r\n")) {
            replaceStr = replaceStr.substring(0, replaceStr.length() - 2);
        }
        content = Kai9comUtils.replaceControlledSection(content, replaceStr, "【制御:開始】対象カラム⑥", "【制御:終了】対象カラム⑥", isTargetStrLeave);

        // 雛形コード取り出し:TextInput
        startTag = "{/* 【制御:開始】対象カラム⑦[TextInput] */}";
        endTag = "{/* 【制御:終了】対象カラム⑦[TextInput] */}";
        results = extractContent(content, startTag, endTag, isTargetStrLeave);
        Line_TextInput = results[0];
        content = results[1];
        // 雛形コード取り出し:TextArea
        startTag = "{/* 【制御:開始】対象カラム⑦[TextArea] */}";
        endTag = "{/* 【制御:終了】対象カラム⑦[TextArea] */}";
        results = extractContent(content, startTag, endTag, isTargetStrLeave);
        String Line_TextArea = results[0];
        content = results[1];
        // 雛形コード取り出し:NumberInput
        startTag = "{/* 【制御:開始】対象カラム⑦[NumberInput] */}";
        endTag = "{/* 【制御:終了】対象カラム⑦[NumberInput] */}";
        results = extractContent(content, startTag, endTag, isTargetStrLeave);
        String Line_NumberInput = results[0];
        content = results[1];
        // 雛形コード取り出し:SelectInput
        startTag = "{/* 【制御:開始】対象カラム⑦[SelectInput] */}";
        endTag = "{/* 【制御:終了】対象カラム⑦[SelectInput] */}";
        results = extractContent(content, startTag, endTag, isTargetStrLeave);
        String Line_SelectInput = results[0];
        content = results[1];
        // 雛形コード取り出し:Checkbox
        startTag = "{/* 【制御:開始】対象カラム⑦[Checkbox] */}";
        endTag = "{/* 【制御:終了】対象カラム⑦[Checkbox] */}";
        results = extractContent(content, startTag, endTag, isTargetStrLeave);
        // Files.writeString(Paths.get("d:\\deleteok1.txt"), content);デバッグ用
        String Line_Checkbox = results[0];
        content = results[1];
        // 型に応じた入力コンポーネントを生成
        insertTag = "              {/* 【制御:挿入行]対象カラム⑦ */}";
        tmpStr = "";
        for (int i = Kai9ComUtils.modelList.size() - 1; i >= 0; i--) {
            Models Models = Kai9ComUtils.modelList.get(i);
            // PKは表示させない(タイトルバーへ表示させる)
            if (Models.is_pk) continue;
            // システムカラムは表示も更新もさせない
            if (Models.columnname.equals("modify_count")) continue;
            if (Models.columnname.equals("update_u_id")) continue;
            if (Models.columnname.equals("update_date")) continue;
            // string
            if (Models.input_type.toUpperCase().equals("TEXTINPUT") || Models.input_type.toUpperCase().equals("TEXTAREA")) {
                // ※注意事項：「ナチュラルキー1」を元ネタとして利用。置換するので、元ネタの置換前コードが書き換えられてると上手く動作しないので注意。
                orignPatternRegexp = "";
                orignPatternMessage = "";
                if (Models.input_type.toUpperCase().equals("TEXTINPUT")) {
                    if (Line_TextInput.isEmpty()) continue;
                    tmpStr = Line_TextInput;
                    tmpStr = tmpStr.replace("natural_key1", Models.columnname);
                    tmpStr = tmpStr.replace("ナチュラルキー1", Models.FieldName_J);
                    tmpStr = tmpStr.replace("maxLength={10}", "maxLength={" + Models.MaxLength + "}");
                    // 日付
                    if (Models.data_type.equals("date")) {
                        tmpStr = tmpStr.replace("value={formState." + Models.columnname + ".value}", "value={moment(formState.date.value).format('YYYY-MM-DD')}");
                        tmpStr = tmpStr.replace("type=\"text\"", "type=\"date\"");
                        tmpStr = Kai9ComUtils.removeLinesContainingTag(tmpStr, "maxLength={0}");
                        tmpStr = Kai9ComUtils.removeLinesContainingTag(tmpStr, "minLength={1}");
                        tmpStr = Kai9ComUtils.removeLinesContainingTag(tmpStr, "Pattern_regexp");
                        tmpStr = Kai9ComUtils.removeLinesContainingTag(tmpStr, "Pattern_message");
                    }
                    // 日時
                    if (Models.data_type.equals("timestamp")) {
                        tmpStr = tmpStr.replace("value={formState." + Models.columnname + ".value}", "value={moment(formState.datetime.value).format('YYYY-MM-DD HH:mm:ss')}");
                        tmpStr = tmpStr.replace("type=\"text\"", "type=\"datetime-local\"");
                        tmpStr = Kai9ComUtils.removeLinesContainingTag(tmpStr, "maxLength={0}");
                        tmpStr = Kai9ComUtils.removeLinesContainingTag(tmpStr, "minLength={1}");
                        tmpStr = Kai9ComUtils.removeLinesContainingTag(tmpStr, "Pattern_regexp");
                        tmpStr = Kai9ComUtils.removeLinesContainingTag(tmpStr, "Pattern_message");
                    }
                    orignPatternRegexp = "Pattern_regexp= \"\"";
                    orignPatternMessage = "Pattern_message= \"\"";
                } else if (Models.input_type.toUpperCase().equals("TEXTAREA")) {
                    if (Line_TextArea.isEmpty()) continue;
                    tmpStr = Line_TextArea;
                    tmpStr = tmpStr.replace("email_address", Models.columnname);
                    tmpStr = tmpStr.replace("メールアドレス", Models.FieldName_J);
                    tmpStr = tmpStr.replace("maxLength={320}", "maxLength={" + Models.MaxLength + "}");
                    orignPatternRegexp = "Pattern_regexp = \"^([a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,})?$\"";
                    orignPatternMessage = "Pattern_message= \"メールアドレスの入力形式が不正です\"";
                    // MaxLengthが0の場合。CLOBなので、maxLengthとminLengthの記載を無くす
                    if (Models.MaxLength + Models.MinLength == 0) {
                        tmpStr = Kai9ComUtils.removeLinesContainingTag(tmpStr, "maxLength");
                        tmpStr = Kai9ComUtils.removeLinesContainingTag(tmpStr, "minLength");
                    }
                }

                // 最小文字数
                if (Models.MinLength != 0) {
                    tmpStr = tmpStr.replaceAll("minLength=\\{\\d+\\}", "minLength={" + Models.MinLength + "}");
                } else {
                    tmpStr = tmpStr.replaceAll(".*minLength=\\{\\d+\\}.*(\r?\n|\r)?", "");
                }
                // 必須
                if (Models.is_not_null) {
                    tmpStr = tmpStr.replace("required={false}", "required={true}");
                }
                // バリデーションチェック
                if (Models.validation_check.equals("全角限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "Pattern_regexp= \"^[^ -~｡-ﾟ]*$\"");
                    tmpStr = tmpStr.replace(orignPatternMessage, "Pattern_message= \"全角で入力して下さい\"");
                } else if (Models.validation_check.equals("半角限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "Pattern_regexp= \"^[ -~｡-ﾟ]*$\"");
                    tmpStr = tmpStr.replace(orignPatternMessage, "Pattern_message= \"半角で入力して下さい\"");
                } else if (Models.validation_check.equals("半角英字限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "Pattern_regexp= \"^[a-zA-Z]*$\"");
                    tmpStr = tmpStr.replace(orignPatternMessage, "Pattern_message= \"半角英字で入力して下さい\"");
                } else if (Models.validation_check.equals("半角数字限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "Pattern_regexp= \"^[0-9]*$\"");
                    tmpStr = tmpStr.replace(orignPatternMessage, "Pattern_message= \"半角数字で入力して下さい\"");
                } else if (Models.validation_check.equals("半角記号限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "Pattern_regexp = {\"^([!\\\"#$%&'()*+,-./:;<=>?@[\\\\]^_`{|}~\\\\\\\\]+)?$\"}");

                    tmpStr = tmpStr.replace(orignPatternMessage, "Pattern_message= \"半角記号で入力して下さい\"");
                } else if (Models.validation_check.equals("半角カナ限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "Pattern_regexp= \"^([ｦ-ﾝﾞﾟ]|ﾞ|ﾟ)*$\"");
                    tmpStr = tmpStr.replace(orignPatternMessage, "Pattern_message= \"半角で入力して下さい\"");
                } else if (Models.validation_check.equals("全角カナ限定")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "Pattern_regexp= \"^[ァ-ヶ]*$\"");
                    tmpStr = tmpStr.replace(orignPatternMessage, "Pattern_message= \"全角カナで入力して下さい\"");
                } else if (Models.validation_check.equals("郵便番号")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "Pattern_regexp = \"^(\\d{3}-?\\d{4})?$\"");
                    tmpStr = tmpStr.replace(orignPatternMessage, "Pattern_message= \"郵便番号の入力形式が不正です\"");
                    // 郵便番号は-も入力時だけは許容するので、DBが7桁で設定されていても、8桁に変更する
                    tmpStr = tmpStr.replace("maxLength={7}", "maxLength={8}");
                } else if (Models.validation_check.equals("電話番号")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "Pattern_regexp = \"^(\\d{2,4}-?\\d{2,4}-?\\d{3,4})?$\"");
                    tmpStr = tmpStr.replace(orignPatternMessage, "Pattern_message= \"電話番号の入力形式が不正です\"");
                } else if (Models.validation_check.equals("メールアドレス")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "Pattern_regexp = \"^([a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,})?$\"");
                    tmpStr = tmpStr.replace(orignPatternMessage, "Pattern_message= \"メールアドレスの入力形式が不正です\"");
                } else if (Models.validation_check.equals("URL")) {
                    tmpStr = tmpStr.replace(orignPatternRegexp, "Pattern_regexp = \"^((https?|ftp):\\/\\/[^\\s/$.?#].[^\\s]*)?$\"");
                    tmpStr = tmpStr.replace(orignPatternMessage, "Pattern_message= \"URLの入力形式が不正です\"");
                } else if (Models.validation_check.contains("【正規表現】")) {
                    String tmpregexp = Models.validation_check.replace("【正規表現】", "");
                    // JSX内の属性に直接正規表現の文字列を記述すると、JSXのパーサーがエスケープ文字を正しく解釈できないことがあるため、正規表現を文字列リテラルとして渡す際に {} を使用して、JavaScriptの式として扱うことで問題を回避
                    tmpStr = tmpStr.replace(orignPatternRegexp, "Pattern_regexp= {\"" + tmpregexp + "\"}");
                    tmpStr = tmpStr.replace(orignPatternMessage, "Pattern_message= \"入力形式が不正です\"");
                }

                if (Models.validation_check.isEmpty()) {
                    // バリデーションチェックが不要の場合、削除する
                    if (!Models.columnname.equals("natural_key1")) {// natural_key1は原本なので残す
                        tmpStr = Kai9ComUtils.removeLinesContainingTag(tmpStr, "Pattern_regexp");
                        tmpStr = Kai9ComUtils.removeLinesContainingTag(tmpStr, "Pattern_message");
                    }
                }
                tmpStr = replaceDisabledCode(Models, tmpStr);// 権限
                tmpStr = replaceRequiredCode(Models, tmpStr);// 必須
                content = content.replace(insertTag, insertTag + RN + tmpStr);
            }
            // number
            if (Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
                if (Line_NumberInput.isEmpty()) continue;
                tmpStr = Line_NumberInput;
                tmpStr = tmpStr.replace("number_limited", Models.columnname);
                tmpStr = tmpStr.replace("数値限定", Models.FieldName_J);
                tmpStr = replaceDisabledCode(Models, tmpStr);// 権限
                tmpStr = replaceRequiredCode(Models, tmpStr);// 必須

                // 最大数値
                if (Models.special_control_max != 0) {
                    tmpStr = tmpStr.replaceAll("max=\\{[^}]+\\}", "max={" + Models.special_control_max + "}");
                } else {
                    // 数値型としての最大値を計算
                    String tmpMax = ScenarioMaker.getNormalValue(Models, ScenarioMaker.LengthPattern.MAX, 0);
                    tmpStr = tmpStr.replaceAll("max=\\{[^}]+\\}", "max={" + tmpMax + "}");
                }
                // 最小数値
                if (Models.special_control_min != 0) {
                    // min_valueが0でない場合、tmpStr内の該当部分を置換する
                    tmpStr = tmpStr.replaceAll("min=\\{[^}]+\\}", "min={" + Models.special_control_min + "}");
                } else {
                    // 数値型としての最小値を計算
                    String tmpMin = ScenarioMaker.getNormalValue(Models, ScenarioMaker.LengthPattern.MIN, 0);
                    tmpStr = tmpStr.replaceAll("min=\\{[^}]+\\}", "min={" + tmpMin + "}");
                }
                content = content.replace(insertTag, insertTag + RN + tmpStr);
            }
            // boolean
            if (Models.input_type.toUpperCase().equals("CHECKBOX")) {
                if (Line_Checkbox.isEmpty()) continue;
                tmpStr = Line_Checkbox;
                tmpStr = tmpStr.replace("flg", Models.columnname);
                tmpStr = tmpStr.replace("フラグ", Models.FieldName_J);
                tmpStr = replaceDisabledCode(Models, tmpStr);// 権限
                content = content.replace(insertTag, insertTag + RN + tmpStr);

            }
            // 特殊制御
            if (Models.input_type.toUpperCase().equals("SELECTINPUT")) {
                if (Line_SelectInput.isEmpty()) continue;
                tmpStr = Line_SelectInput;
                tmpStr = tmpStr.replace("関連ID", Models.FieldName_J);
                tmpStr = tmpStr.replace("formState.related_pk.value", "formState." + Models.columnname + ".value");
                Relations RelationsTmp = parse_Relations(Models.special_control_relation, Models.columnname);
                // テーブル名の末尾から「_a」又は「_b」を取り除く
                String table_name = RelationsTmp.tableA;
                if (table_name.endsWith("_a") || table_name.endsWith("_b")) {
                    table_name = table_name.substring(0, table_name.length() - 2);
                }
                tmpStr = tmpStr.replace("SelectInput_related_table_related_pk_related_data_s", "SelectInput_" + table_name + "_" + RelationsTmp.columnA + "_" + RelationsTmp.columnB + "_s");
                tmpStr = tmpStr.replace("related_pk", Models.columnname);
                tmpStr = replaceDisabledCode(Models, tmpStr);// 権限
                tmpStr = replaceRequiredCode(Models, tmpStr);// 必須
                content = content.replace(insertTag, insertTag + RN + tmpStr);
            }
        }
        if (!isTargetStrLeave) {
            content = content.replace("{/* 【制御:挿入行]対象カラム⑦ */}", "");
        }

        // 置換
        content = content.replace(src_classname, classname);
        content = content.replace(src_Classname, Classname);
        content = content.replace("シングル表", lTable_Name_J);
        // UTF-8 エンコーディングでファイルを上書き保存
        Files.write(Paths.get(file.toURI()), content.getBytes(StandardCharsets.UTF_8));

        // ======================================================================
        // その他_追記用
        // ======================================================================
        File Other_file = new File(OurDir + "\\React\\" + Classname + "追記用.txt");
        OutputStreamWriter osw_Other = new OutputStreamWriter(new FileOutputStream(Other_file), "UTF-8");
        try (BufferedWriter bw = new BufferedWriter(osw_Other)) {
            bw.write("------------------------------------------------" + RN);
            bw.write("App.tsx" + RN);
            bw.write("------------------------------------------------" + RN);
            bw.write("    const " + Classname + " = lazy(() => import(\"./components/" + Classname + "/" + Classname + "\"));" + RN);
            bw.write("" + RN);
            bw.write("    <Route path=\"/" + classname + "\" element={<" + Classname + "/>} />" + RN);
            bw.write("" + RN);
            bw.write("------------------------------------------------" + RN);
            bw.write("store\\Models\\root.interface.ts" + RN);
            bw.write("------------------------------------------------" + RN);
            bw.write("    import { I" + Classname + "," + Classname + "ModificationStatus } from \"./" + classname + ".interface\";" + RN);
            bw.write("" + RN);
            bw.write("export interface I" + Classname + "State {" + RN);
            bw.write("    " + Classname + "s: I" + Classname + "[];" + RN);
            bw.write("    selected" + Classname + ": I" + Classname + " | null;" + RN);
            bw.write("    modificationState: " + Classname + "ModificationStatus;" + RN);
            bw.write("    IsFirst: boolean;" + RN);
            bw.write("    all_count: number;" + RN);
            if (classname.equals("m_user")) {// ユーザマスタ専用
                bw.write("    admin_count: number;" + RN);
                bw.write("    normal_count: number;" + RN);
                bw.write("    readonly_count: number;" + RN);
            }
            bw.write("    " + Classname + "Historys: I" + Classname + "[];" + RN);
            bw.write("}" + RN);
            bw.write("" + RN);
            bw.write("export interface I" + Classname + "PagenationState {" + RN);
            bw.write("    CurrentPage: number;" + RN);
            bw.write("    numberOfDisplaysPerpage: number;" + RN);
            bw.write("}" + RN);
            bw.write("" + RN);
            bw.write("    >>>>>※ export interface IStateType {　内" + RN);
            bw.write("    " + classname + "s: I" + Classname + "State;" + RN);
            bw.write("    " + classname + "Pagenation: I" + Classname + "PagenationState;" + RN);
            bw.write("" + RN);
            bw.write("------------------------------------------------" + RN);
            bw.write("store\\reducers\\root.reducer.ts" + RN);
            bw.write("------------------------------------------------" + RN);
            bw.write("    import " + classname + "Reducer from \"./" + classname + ".reducer\";" + RN);
            bw.write("    import " + classname + "PagenationReducer from \"./" + classname + "Pagenation.reducer\";" + RN);
            bw.write("" + RN);
            bw.write("    >>>>>※const rootReducers: Reducer<IStateType> = combineReducers({  内" + RN);
            bw.write("    " + classname + "s: " + classname + "Reducer," + RN);
            bw.write("    " + classname + "Pagenation: " + classname + "PagenationReducer," + RN);
            bw.write("" + RN);
            bw.write("------------------------------------------------" + RN);
            bw.write("components\\LeftMenu\\LeftMenu.tsx" + RN);
            bw.write("------------------------------------------------" + RN);
            bw.write("                <li className={`nav-item ${styles.navItem}`}>" + RN);
            bw.write("                    <Link className={`nav-link ${styles.navLink}`} to=\"/" + classname + "\">" + RN);
            bw.write("                        >>>>>※アイコンを変更" + RN);
            bw.write("                        <i className=\"fas fa-fw fa-user\"></i>" + RN);
            bw.write("                        <span>" + lTable_Name_J + "</span>" + RN);
            bw.write("                    </Link>" + RN);
            bw.write("                </li>" + RN);
            bw.write("" + RN);
            bw.write("" + RN);
            bw.write("------------------------------------------------" + RN);
            bw.write("non_common\\types\\Form.types.ts" + RN);
            bw.write("------------------------------------------------" + RN);
            bw.write("    export interface I" + Classname + "FormState {" + RN);
            for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
                Models Models = Kai9ComUtils.modelList.get(i);
                bw.write("        " + Models.columnname + ": IFormStateField<" + CnvType(Models.data_type) + ">;" + RN);
            }
            bw.write("        //(非DB項目)" + RN);
            bw.write("        update_user: IFormStateField<string>;" + RN);
            bw.write("    };" + RN);
        }

        return true;
    }

    // 雛形コードの取り出し用
    // content中で最初に見つかったstartTagとendTagに囲まれた部分を一度だけ抽出し、抽出後はマッチする全対象をcontentから削除する。複数のマッチが存在する場合、最初のマッチ対象だけをextractedLineとして返す。
    public static String[] extractContent(String content, String startTag, String endTag, boolean isTargetStrLeave) {
        // 開始タグが存在する行(前方の空白やタブ含む)と、終了タグ(後方の空白やタブ含む)が存在する行までを抽出
        Pattern pattern = Pattern.compile("^(\\s*)" + Pattern.quote(startTag) + "(.*?)" + Pattern.quote(endTag) + "(\\s*)\\R", Pattern.DOTALL | Pattern.MULTILINE);

        Matcher matcher = pattern.matcher(content);
        String extractedLine = "";
        if (matcher.find()) {
            String leadingSpaces = matcher.group(1); // 開始タグが存在する行の先頭の空白を取得
            if (!isTargetStrLeave) {
                // タグを取り除く場合
                extractedLine = leadingSpaces + matcher.group(2); // 先頭の空白と内容だけを結合
            } else {
                extractedLine = matcher.group(0); // 全体を取得
            }
            // 全コンテンツから抽出部分を削除
            content = matcher.replaceAll("");
        }
        // 文字列の先頭と末尾の空白行を削除
        extractedLine = extractedLine.replaceAll("\\s*[\\r\\n]+$", "");
        return new String[] { extractedLine, content };
    }

    /**
     * 型マッピング(PostgreSQL→Java) https://beanql.osdn.jp/type_map.html
     */
    public static String CnvType(String data_type) {
        String lDataType2 = "型不明";
        if (data_type.contains("varchar")) {
            data_type = "varchar";
        } else if (data_type.contains("character")) {
            data_type = "character";
        } else if (data_type.contains("char")) {
            data_type = "character";
        } else if (data_type.contains("numeric")) {
            data_type = "numeric";
        }

        switch (data_type) {
        case "boolean":
            lDataType2 = "boolean";
            break;
        case "smallint":
            lDataType2 = "number";
            break;
        case "integer":
            lDataType2 = "number";
            break;
        case "bigint":
            lDataType2 = "number";
            break;
        case "real":
            lDataType2 = "number";
            break;
        case "double precision":
            lDataType2 = "number";
            break;
        case "numeric":
            lDataType2 = "number";
            break;
        case "text":
            lDataType2 = "string";
            break;
        case "varchar":
            lDataType2 = "string";
            break;
        case "character":
            lDataType2 = "string";
            break;
        case "bytea":
            lDataType2 = "Blob";
            break;// Uint8Arrayを使った事が無いので未検証
        case "timestamp":
            lDataType2 = "Date";
            break;
        case "date":
            lDataType2 = "Date";
            break;
        case "time":
            lDataType2 = "Date";
            break;
        case "smallserial":
            lDataType2 = "number";
            break;
        case "serial":
            lDataType2 = "number";
            break;
        case "bigserial ":
            lDataType2 = "number";
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

    // [relation]テーブル名A.カラム名A:テーブル名B.カラム名B の構文で記載された文字列から、各要素を取り出す
    // 例)【relation】related_table_a.related_pk:related_table_a.related_data
    public static Relations parse_Relations(String input, String src_column) {
        // 正規表現パターンを定義
        String pattern = "【(.*?)】(.*?)\\.(.*?):(.*?)\\.(.*?)";

        // パターンをコンパイル
        Pattern regex = Pattern.compile(pattern);

        // パターンと入力文字列をマッチさせる
        Matcher matcher = regex.matcher(input);

        // マッチした場合、各要素を取得して構造体で返す
        if (matcher.matches()) {
            String tableNameA = matcher.group(2).toLowerCase(); // テーブル名A
            String columnNameA = matcher.group(3).toLowerCase(); // カラム名A
            String tableNameB = matcher.group(4).toLowerCase(); // テーブル名B
            String columnNameB = matcher.group(5).toLowerCase(); // カラム名B

            return new Relations(tableNameA, columnNameA, tableNameB, columnNameB, src_column);
        } else {
            // マッチしない場合はnullを返す
            return null;
        }
    }

    /**
     * 権限に基づいてTypeScriptのdisabled属性の値を動的に生成し、指定された文字列に置換するメソッド。
     * 編集不可の権限が何れにも無い場合は、disable属性の個所を削除する。
     * 
     * @param Models 権限フラグを含むモデルオブジェクト。
     * @param tmpStr 置換対象の文字列。
     * @return 置換または削除後の文字列を返す。
     */
    public static String replaceDisabledCode(Models Models, String tmpStr) {
        // 置換するデフォルトのTypeScriptコード
        String defaultCode = "disabled={[AUT_NUM_READ_ONLY].includes(account.authority_lv)}//[参照専用]の場合は編集不可";

        // 何れの権限も編集不可でない場合、デフォルトコードを削除
        if (Models.is_mod_admin && Models.is_mod_normal && Models.is_mod_readonly) {
            return tmpStr.replace(defaultCode, "");
        }

        // 管理者、一般、参照専用のいずれかに編集不可がある場合にのみ処理を行う
        if (!Models.is_mod_admin || !Models.is_mod_normal || !Models.is_mod_readonly) {
            StringJoiner authJoiner1 = new StringJoiner(", ");
            StringJoiner authJoiner2 = new StringJoiner(", ");

            // 各権限が無効である場合、対応する権限名とラベルを追加
            if (!Models.is_mod_admin) {
                authJoiner1.add("AUT_NUM_ADMIN");
                authJoiner2.add("[管理者]");
            }
            if (!Models.is_mod_normal) {
                authJoiner1.add("AUT_NUM_NORMAL");
                authJoiner2.add("[一般]");
            }
            if (!Models.is_mod_readonly) {
                authJoiner1.add("AUT_NUM_READ_ONLY");
                authJoiner2.add("[参照専用]");
            }

            // TypeScriptコードを生成し、デフォルトのコードと置換
            String newDisabledCode = "disabled={[" + authJoiner1.toString().replaceAll(", ", ", ") +
                    "].includes(account.authority_lv)}//" + authJoiner2.toString() + "の場合は編集不可";
            return tmpStr.replace(defaultCode, newDisabledCode);
        }

        // 何れの条件も満たさない場合、変更なしで返す
        return tmpStr;
    }

    /**
     * 必須の要件に基づいてTypeScriptのrequired属性の値を動的に生成し、指定された文字列に置換するメソッド。
     * 
     * @param Models 必須の要件を含むモデルオブジェクト。
     * @param tmpStr 置換対象の文字列。
     * @return 置換または削除後の文字列を返す。
     */
    public static String replaceRequiredCode(Models Models, String tmpStr) {
        // 置換するデフォルトのTypeScriptコード
        String defaultCode1 = "required={false}";
        String defaultCode2 = "required={true}";

        String retuenStr = tmpStr;

        if (Models.is_not_null) {
            // 必須の場合
            retuenStr = retuenStr.replace(defaultCode1, defaultCode2);
        } else {
            // 必須ではない場合
            retuenStr = retuenStr.replace(defaultCode2, defaultCode1);
        }
        return retuenStr;
    }

    public static String removeDecimalPart(String MinLength) {
        try {
            double value = Double.parseDouble(MinLength);
            if (value == (int) value) {
                // 小数点以下が0の場合は整数として出力
                return String.valueOf((int) value);
            } else {
                // 小数点以下が0でない場合はそのまま文字列として出力
                return String.valueOf(value);
            }
        } catch (NumberFormatException e) {
            // MinLengthが数値ではない場合、元の文字列を返す
            return MinLength;
        }
    }

}
