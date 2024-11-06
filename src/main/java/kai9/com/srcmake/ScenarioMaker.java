package kai9.com.srcmake;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jasypt.encryption.StringEncryptor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetView;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetViews;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import kai9.libs.Kai9Util;
import kai9.libs.Kai9Utils;
import kai9.libs.PoiUtil;
import kai9.libs.TimeMeasurement;
import com.github.javafaker.Faker;
import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import kai9.com.model.AppEnv;
import kai9.com.service.AppEnv_Service;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

@Component
public class ScenarioMaker {

    @Autowired
    private StringEncryptor encryptor;

    @Autowired
    @Lazy
    PasswordEncoder passwordEncoder;

    @Autowired
    private AppEnv_Service AppEnv_Service;

    private static String RN = "\r\n";

    // エラー格納リスト
    private static List<String> Errors = new ArrayList<>();

    private boolean IsError = false;

    // 制御対象の列番号用
    @SuppressWarnings("unused")
    private static int col_step = 0;
    private static int col_process_content = 0;
    private static int col_comment = 0;
    private static int col_total_count = 0;
    private static int col_keyword = 0;
    private static int col_value1 = 0;
    private static int col_value2 = 0;
    private static int col_value3 = 0;
    @SuppressWarnings("unused")
    private static int col_variable = 0;
    private static int col_expected_result = 0;
    @SuppressWarnings("unused")
    private static int col_actual_result = 0;
    @SuppressWarnings("unused")
    private static int col_stop_on_fail = 0;
    @SuppressWarnings("unused")
    private static int col_start = 0;
    @SuppressWarnings("unused")
    private static int col_end = 0;
    @SuppressWarnings("unused")
    private static int col_duration = 0;
    private static int col_log = 0;

    // ユニークインデックスの連番用
    private static int unique_index = 0;
    // 検索キー格納用
    private static String findKey = "";
    private static String findKey_columnname = "";
    private static int findKeyLevel = 0;

    // 丸め処理用
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##############################");

    public enum LengthPattern {
        MAX, MIN, NORMAL
    }

    // ユニークキー格納用
    static LinkedHashMap<String, String> unique_index_Map = new LinkedHashMap<>();

    // デバッグ用
    static boolean isDebug = true;
    XSSFWorkbook workbook_Debug = null;

    public void Make(Workbook excel, Sheet pWs, String OurDir, String projectName, boolean isTargetStrLeave)
            throws IOException {
        IsError = false;

        // テーブル定義書のフォーマットに関する各エラーチェックは、React側で実施するので、こちらでは行わない。

        TimeMeasurement.clear();
        TimeMeasurement.logTimeStart("前準備", isDebug);// 時間計測

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

        @SuppressWarnings("unused")
        boolean is_bytea_exist = false;// BLOBの存在確認用
        @SuppressWarnings("unused")
        boolean is_date_exist = false;// 日付型の存在確認用
        @SuppressWarnings("unused")
        boolean is_generated_exist = false;// 自動採番型の存在確認用
        @SuppressWarnings("unused")
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
                if (!ColumnName.contains("modify_count")) {// modify_countにはIDを付けない
                    id_count++;
                }
            }
        }
        TimeMeasurement.logTimeEnd("前準備", isDebug);// 時間計測

        // 雛形の「処理シナリオ」を開く
        TimeMeasurement.logTimeStart("処理シナリオ作成", isDebug);// 時間計測
        AppEnv m_env = AppEnv_Service.findById();
        // URLの最後の部分（ファイル名）を取得
        String fileName = m_env.getSvn_scenario_url().substring(m_env.getSvn_scenario_url().lastIndexOf('/') + 1);
        try {
            // URLデコード
            fileName = URLDecoder.decode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        String fullPath = FilenameUtils.concat(m_env.getSvn_scenario_dir(), fileName);
        Path path = Paths.get(fullPath);
        if (!Files.exists(path)) {
            Errors.add("処理シナリオが存在しません: " + fullPath + RN);
            Kai9Util.ErrorMsg(String.join("", Errors));
            return;
        }

        // 読み取り専用で開く
        XSSFWorkbook workbook = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fullPath);
            workbook = new XSSFWorkbook(fis);
            workbook_Debug = workbook;

            // -------------------------------------------------------------------
            // 「実行順」シート
            // -------------------------------------------------------------------
            TimeMeasurement.logTimeStart("処理シナリオ作成:「実行順」シート", isDebug);// 時間計測
            String sheetname = "実行順";
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet(sheetname);
            if (sheet == null) {
                Errors.add("出力に失敗しました。「" + sheetname + "」シートが存在しません。" + RN);
                Kai9Util.ErrorMsg(String.join("", Errors));
                return;
            }
            // 制御文字の検索
            int row1 = findControlCharacterRow(sheet, sheetname, "#R1#");
            int row2 = findControlCharacterRow(sheet, sheetname, "#R2#");
            int row3 = findControlCharacterRow(sheet, sheetname, "#R3#");
            int row4 = findControlCharacterRow(sheet, sheetname, "#R4#");
            int row5 = findControlCharacterRow(sheet, sheetname, "#R5#");

            // 「処理No」を空欄にする
            setSellValue(sheet, sheetname, row1, "処理No", +1, "");
            // 「処理名」にテーブル名を入れる
            setSellValue(sheet, sheetname, row1, "処理名", +1, lTable_Name_J);
            // 「実行時引数」を空欄にする
            setSellValue(sheet, sheetname, row1, "実行時引数", +1, "");
            // 「実行ホスト」を空欄にする
            setSellValue(sheet, sheetname, row2, "実行ホスト", +1, "");
            // 「実行時刻」を空欄にする
            setSellValue(sheet, sheetname, row2, "実行時刻", +1, "");
            // 「備考」を空欄にする
            setSellValue(sheet, sheetname, row5, "備考", +1, "");

            // 「実施FLG」をTRUEにする ※シートは1と2決め打ち
            int col_exec_flg = findControlCharacterCol(sheet, sheetname, row3, "実施FLG");
            sheet.getRow(row4 + 0).getCell(col_exec_flg).setCellValue("TRUE");
            sheet.getRow(row4 + 1).getCell(col_exec_flg).setCellValue("TRUE");
            TimeMeasurement.logTimeEnd("処理シナリオ作成:「実行順」シート", isDebug);// 時間計測

            String pw = "";
            // 「2」「3」シートでループ(共通のロジックがあるので）
            for (int sheet_roop_count = 2; sheet_roop_count <= 3; sheet_roop_count++) {
                // -------------------------------------------------------------------
                // 「2」シート:CRUDテスト
                // -------------------------------------------------------------------
                // -------------------------------------------------------------------
                // 「3」シート：バリデーションテスト
                // -------------------------------------------------------------------
                TimeMeasurement.logTimeStart("処理シナリオ作成:「2」「3」シート準備", isDebug);// 時間計測

                sheetname = String.valueOf(sheet_roop_count);
                sheet = workbook.getSheet(sheetname);
                if (sheet == null) {
                    Errors.add("出力に失敗しました。「" + sheetname + "」シートが存在しません。" + RN);
                    Kai9Util.ErrorMsg(String.join("", Errors));
                    return;
                }
                // 制御文字の検索
                int row7 = findControlCharacterRow(sheet, sheetname, "#R7#");
                row3 = findControlCharacterRow(sheet, sheetname, "#R3#");
                int col1 = findControlCharacterCol(sheet, sheetname, 0, "#C1#");

                // 「処理概要」を入力する
                if (sheet_roop_count == 2) {
                    sheet.getRow(row7).getCell(col1).setCellValue(lTable_Name_J + "の新規、変更、削除、読取");
                } else if (sheet_roop_count == 3) {
                    sheet.getRow(row7).getCell(col1).setCellValue(lTable_Name_J + "の新規");
                }

                // 明細
                // 制御対象の列番号を取得
                col_step = findControlCharacterCol(sheet, sheetname, row3, "Step");
                col_process_content = findControlCharacterCol(sheet, sheetname, row3, "処理内容");
                col_comment = findControlCharacterCol(sheet, sheetname, row3, "コメント");
                col_total_count = findControlCharacterCol(sheet, sheetname, row3, "集計数");
                col_keyword = findControlCharacterCol(sheet, sheetname, row3, "キーワード");
                col_value1 = findControlCharacterCol(sheet, sheetname, row3, "値1");
                col_value2 = findControlCharacterCol(sheet, sheetname, row3, "値2");
                col_value3 = findControlCharacterCol(sheet, sheetname, row3, "値3");
                col_variable = findControlCharacterCol(sheet, sheetname, row3, "変数");
                col_expected_result = findControlCharacterCol(sheet, sheetname, row3, "想定結果");
                col_actual_result = findControlCharacterCol(sheet, sheetname, row3, "実施結果");
                col_stop_on_fail = findControlCharacterCol(sheet, sheetname, row3, "想定相違で停止");
                col_start = findControlCharacterCol(sheet, sheetname, row3, "開始");
                col_end = findControlCharacterCol(sheet, sheetname, row3, "終了");
                col_duration = findControlCharacterCol(sheet, sheetname, row3, "所要時間");
                col_log = findControlCharacterCol(sheet, sheetname, row3, "ログ");

                // 【制御】新規:入力
                unique_index = 1;// ユニークインデックスの連番用
                // 検索キー初期化
                findKey = "";
                findKey_columnname = "";
                findKeyLevel = 0;

                // 【制御】ログイン:PW
                // パスワード
                if (sheet_roop_count == 2) { // シート2のPWを3でも揃える
                    pw = generateRandomPassword(12);
                }
                // 暗号化
                String encrypt_pw = encryptor.encrypt(pw);
                int pwRow = findControlCharacterRow(sheet, sheetname, "【制御】ログイン:PW");
                if (pwRow != -1) {
                    sheet.getRow(pwRow).getCell(col_value2).setCellValue("<暗号化>" + encrypt_pw + "</暗号化>");
                    // デバッグ用：パスワードを平分でログに記載
                    // sheet.getRow(pwRow).getCell(col_log).setCellValue(pw);
                }

                // 【制御】新規:準備SQL1
                String pkColumnName = "";
                for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
                    Models Models = Kai9ComUtils.modelList.get(i);
                    if (!Models.is_pk)
                        continue;
                    pkColumnName = Models.columnname;
                    break;
                }
                int sqlRow = findControlCharacterRow(sheet, sheetname, "【制御】新規:準備SQL1");
                if (sqlRow != -1) {
                    sheet.getRow(sqlRow).getCell(col_value1).setCellValue("delete from " + lTable_Name);
                }
                // 【制御】新規:準備SQL2
                sqlRow = findControlCharacterRow(sheet, sheetname, "【制御】新規:準備SQL2");
                if (sqlRow != -1) {
                    sheet.getRow(sqlRow).getCell(col_value1)
                            .setCellValue("delete from " + lTable_Name.replaceAll("_a$", "_b"));
                }
                // 【制御】新規:準備SQL3
                sqlRow = findControlCharacterRow(sheet, sheetname, "【制御】新規:準備SQL3");
                if (sqlRow != -1) {
                    sheet.getRow(sqlRow).getCell(col_value1).setCellValue(
                            "ALTER SEQUENCE " + lTable_Name + "_" + pkColumnName + "_seq RESTART WITH 1 ");
                }
                // 【制御】新規:準備SQL4
                sqlRow = findControlCharacterRow(sheet, sheetname, "【制御】新規:準備SQL4");
                if (sqlRow != -1) {
                    sheet.getRow(sqlRow).getCell(col_value1).setCellValue("ALTER SEQUENCE "
                            + lTable_Name.replaceAll("_a$", "_b") + "_" + pkColumnName + "_seq RESTART WITH 1 ");
                }

                // 【制御】新規:準備SQL5
                // 【制御】新規:準備SQL6
                // 【制御】変更:SQL1
                // 【制御】変更:SQL2
                // 【制御】新規2:SQL1
                // 【制御】新規2:SQL2
                String value = "0";
                for (int roop_count = 1; roop_count <= 6; roop_count++) {
                    if (sheet_roop_count == 3 && roop_count >= 3)
                        continue;// シート3は新規のみ扱う
                    String sqlLabel = "";
                    if (roop_count == 1) {
                        sqlLabel = "【制御】新規:準備SQL5";
                        value = "1";
                    } else if (roop_count == 2) {
                        sqlLabel = "【制御】新規:準備SQL6";
                    } else if (roop_count == 3) {
                        value = "2";
                        sqlLabel = "【制御】変更:SQL1";
                    } else if (roop_count == 4) {
                        sqlLabel = "【制御】変更:SQL2";
                    } else if (roop_count == 5) {
                        value = "3";
                        sqlLabel = "【制御】新規2:SQL1";
                    } else if (roop_count == 6) {
                        sqlLabel = "【制御】新規2:SQL2";
                    }
                    sqlRow = findControlCharacterRow(sheet, sheetname, sqlLabel);
                    if (sqlRow != -1) {
                        if (Kai9ComUtils.RelationsList.size() == 0) {
                            // relation対象が無い場合、行削除
                            PoiUtil.removeRow(sheet, sqlRow);
                        } else {
                            // relation対象全てのSQLを作成
                            List<String> queryList = new ArrayList<>();
                            for (int i = Kai9ComUtils.RelationsList.size() - 1; i >= 0; i--) {// 逆順ループ
                                Relations relation = Kai9ComUtils.RelationsList.get(i);
                                // ループ2週目は履歴テーブル用なので、relation.tableAの末尾の「_a」を「_b」に置換する
                                String tableA = roop_count % 2 != 0 ? relation.tableA
                                        : relation.tableA.replaceAll("_a$", "_b");
                                String query = "delete from " + tableA + " where " + relation.columnA + "::text = '" + value + "'::text";

                                // 重複排除
                                if (!queryList.contains(query)) {
                                    queryList.add(query);
                                }
                            }
                            for (int i = 0; i < queryList.size(); i++) {
                                String query = queryList.get(i);

                                if (i != 0) {
                                    // 2つめ以降の場合、新しい行を挿入する
                                    PoiUtil.insertRow(sheet, sqlRow + 1);
                                    // 新しい行の書式をコピーする
                                    if (sqlRow > 0) {
                                        PoiUtil.copyRowFormatting(sheet, sqlRow, sqlRow + 1);
                                    }
                                }
                                // SQLをセット
                                sheet.getRow(sqlRow).getCell(col_value1).setCellValue(query);
                            }
                        }
                    }
                }

                // 【制御】新規:準備SQL7
                // 【制御】新規:準備SQL8
                // 【制御】変更:SQL3
                // 【制御】変更:SQL4
                // 【制御】新規2:SQL3
                // 【制御】新規2:SQL4
                String query = "";
                value = "0";
                for (int roop_count = 1; roop_count <= 6; roop_count++) {
                    if (sheet_roop_count == 3 && roop_count >= 3)
                        continue;// シート3は新規のみ扱う
                    String sqlLabel = "";
                    if (roop_count == 1) {
                        value = "1";
                        sqlLabel = "【制御】新規:準備SQL7";
                    } else if (roop_count == 2) {
                        sqlLabel = "【制御】新規:準備SQL8";
                    } else if (roop_count == 3) {
                        value = "2";
                        sqlLabel = "【制御】変更:SQL3";
                    } else if (roop_count == 4) {
                        sqlLabel = "【制御】変更:SQL4";
                    } else if (roop_count == 5) {
                        value = "3";
                        sqlLabel = "【制御】新規2:SQL3";
                    } else if (roop_count == 6) {
                        sqlLabel = "【制御】新規2:SQL4";
                    }
                    sqlRow = findControlCharacterRow(sheet, sheetname, sqlLabel);
                    if (sqlRow != -1) {
                        if (Kai9ComUtils.RelationsList.size() == 0) {
                            // relation対象が無い場合、行削除
                            PoiUtil.removeRow(sheet, sqlRow);
                        } else {
                            // relation対象全てのSQLを作成
                            List<String> queryList = new ArrayList<>();
                            for (int i = Kai9ComUtils.RelationsList.size() - 1; i >= 0; i--) {// 逆順ループ
                                Relations relation = Kai9ComUtils.RelationsList.get(i);
                                // ループ2週目は履歴テーブル用なので、relation.tableAの末尾の「_a」を「_b」に置換する
                                String tableA = roop_count % 2 != 0 ? relation.tableA
                                        : relation.tableA.replaceAll("_a$", "_b");

                                // 対象テーブルのDDLを取得
                                String ddl = "";
                                for (int i1 = 1; i1 < excel.getNumberOfSheets(); i1++) {
                                    Sheet ws = excel.getSheetAt(i1);
                                    // 制御文字が無いシートは無視
                                    int r2 = PoiUtil.findRow(ws, "#R2#");
                                    if (r2 == -1)
                                        continue;
                                    int r4 = PoiUtil.findRow(ws, "#R4#");
                                    if (r4 == -1)
                                        continue;
                                    int c_ddlA = PoiUtil.findCol(ws, r4 - 1, "DDL_A");
                                    if (c_ddlA == -1)
                                        continue;
                                    int c_name = PoiUtil.findCol(ws, r2, "テーブル名");
                                    if (c_name == -1)
                                        continue;

                                    // テーブル名
                                    String table_name = PoiUtil.GetStringValue(ws, r2, c_name + 2);
                                    if (!relation.tableA.equals(table_name))
                                        continue;

                                    StringBuilder sb = new StringBuilder();
                                    // DDL_A
                                    int lastRow = PoiUtil.getLastRowNumInColumn(ws, c_ddlA);
                                    for (int r = r4; r <= lastRow; r++) {
                                        sb.append(PoiUtil.GetStringValue(ws, r, c_ddlA)).append("\r\n");
                                    }
                                    sb.append("\r\n");
                                    ddl = sb.toString();
                                }

                                try {
                                    // DDL文を解析してCreateTableオブジェクトを取得
                                    CreateTable createTable = (CreateTable) CCJSqlParserUtil.parse(ddl);
                                    // テーブル名を取得
                                    String tableName = createTable.getTable().getName();
                                    // カラム定義を取得
                                    List<ColumnDefinition> columnDefinitions = createTable.getColumnDefinitions();
                                    // INSERT文を生成
                                    query = generateInsertStatement(tableName, columnDefinitions, relation.columnA, value);
                                    if (roop_count % 2 == 0) {
                                        query = query.replace(relation.tableA, tableA);
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                // 重複排除
                                if (!queryList.contains(query)) {
                                    queryList.add(query);
                                }
                            }
                            for (int i = 0; i < queryList.size(); i++) {
                                String sql = queryList.get(i);

                                if (i != 0) {
                                    // 2つめ以降の場合、新しい行を挿入する
                                    PoiUtil.insertRow(sheet, sqlRow + 1);
                                    // 新しい行の書式をコピーする
                                    if (sqlRow > 0) {
                                        PoiUtil.copyRowFormatting(sheet, sqlRow, sqlRow + 1);
                                    }
                                }
                                // SQLをセット
                                sheet.getRow(sqlRow).getCell(col_value1).setCellValue(sql);
                            }
                        }
                    }
                }

                // 【制御】新規:URL
                int urlRow = findControlCharacterRow(sheet, sheetname, "【制御】新規:URL");
                if (urlRow != -1) {
                    String formula = sheet.getRow(urlRow).getCell(col_value1).getCellFormula();
                    String updatedFormula = formula.replace("single_table", lTable_Name.replaceAll("_a$", ""));
                    sheet.getRow(urlRow).getCell(col_value1).setCellFormula(updatedFormula);
                }
                TimeMeasurement.logTimeEnd("処理シナリオ作成:「2」「3」シート準備", isDebug);// 時間計測

                // シート「2」
                TimeMeasurement.logTimeStart("処理シナリオ作成:「2」シート", isDebug);// 時間計測
                if (sheet_roop_count == 2) {
                    // カラム単位のテスト明細作成
                    unique_index_Map.clear();
                    makeTestRow1(sheet, "【制御】新規:入力", isTargetStrLeave, "ケース1", "新規");

                    // 【制御】新規:登録確認
                    int tmpRow1 = findControlCharacterRow(sheet, sheetname, "【制御】新規:登録確認");
                    // 【制御】変更:登録確認
                    int tmpRow2 = findControlCharacterRow(sheet, sheetname, "【制御】変更:登録確認");
                    // 【制御】削除:登録確認
                    int tmpRow3 = findControlCharacterRow(sheet, sheetname, "【制御】削除:登録確認");
                    // 【制御】検索:準備
                    int tmpRow4 = findControlCharacterRow(sheet, sheetname, "【制御】検索:準備");
                    for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
                        Models Models = Kai9ComUtils.modelList.get(i);
                        if (!Models.is_pk)
                            continue;
                        if (tmpRow1 != -1) {
                            sheet.getRow(tmpRow1).getCell(col_value1)
                                    .setCellValue("【" + Models.FieldName_J + "=1】登録しました");
                            // 制御文字を消す
                            clearControlCharacters(sheet, tmpRow1, isTargetStrLeave);
                        }
                        if (tmpRow2 != -1) {
                            sheet.getRow(tmpRow2).getCell(col_value1)
                                    .setCellValue("【" + Models.FieldName_J + "=1】登録しました");
                            clearControlCharacters(sheet, tmpRow2, isTargetStrLeave);
                        }
                        if (tmpRow3 != -1) {
                            sheet.getRow(tmpRow3).getCell(col_value1)
                                    .setCellValue("【" + Models.FieldName_J + "=1】を削除しました");
                            clearControlCharacters(sheet, tmpRow3, isTargetStrLeave);
                        }
                        if (tmpRow4 != -1) {
                            sheet.getRow(tmpRow4).getCell(col_value1)
                                    .setCellValue("【" + Models.FieldName_J + "=2】登録しました");
                            clearControlCharacters(sheet, tmpRow4, isTargetStrLeave);
                        }
                        break;
                    }
                    // 【制御】変更:検索
                    // 制御対象の行を削除
                    int tmpRow = findControlCharacterRow(sheet, sheetname, "【制御】変更:検索");
                    if (tmpRow != -1) {
                        sheet.getRow(tmpRow).getCell(col_value1).setCellValue(findKey);
                        // 制御文字を消す
                        clearControlCharacters(sheet, tmpRow, isTargetStrLeave);
                    }

                    // 【制御】変更:入力
                    unique_index = 2;// ユニークインデックスの連番用
                    // 検索キー初期化
                    findKey = "";
                    findKey_columnname = "";
                    findKeyLevel = 0;
                    unique_index_Map.clear();
                    makeTestRow1(sheet, "【制御】変更:入力", isTargetStrLeave, "ケース2", "変更");

                    // 【制御】削除:検索
                    tmpRow = findControlCharacterRow(sheet, sheetname, "【制御】削除:検索");
                    if (tmpRow != -1) {
                        sheet.getRow(tmpRow).getCell(col_value1).setCellValue(findKey);
                        // 制御文字を消す
                        clearControlCharacters(sheet, tmpRow, isTargetStrLeave);
                    }

                    // 【制御】検索:検索
                    tmpRow = findControlCharacterRow(sheet, sheetname, "【制御】検索:検索");
                    if (tmpRow != -1) {
                        sheet.getRow(tmpRow).getCell(col_value2).setCellValue(findKey);
                        // 制御文字を消す
                        clearControlCharacters(sheet, tmpRow, isTargetStrLeave);
                    }

                    // 【制御】検索:確認1
                    tmpRow = findControlCharacterRow(sheet, sheetname, "【制御】検索:確認1");
                    if (tmpRow != -1) {
                        sheet.getRow(tmpRow).getCell(col_value1).setCellValue(findKey);
                        // 制御文字を消す
                        clearControlCharacters(sheet, tmpRow, isTargetStrLeave);
                    }

                    // 【制御】検索:確認2
                    tmpRow = findControlCharacterRow(sheet, sheetname, "【制御】検索:確認2");
                    if (tmpRow != -1) {
                        sheet.getRow(tmpRow).getCell(col_value1).setCellValue(findKey_columnname);
                        sheet.getRow(tmpRow).getCell(col_value2).setCellValue(findKey);
                        // 制御文字を消す
                        clearControlCharacters(sheet, tmpRow, isTargetStrLeave);
                    }

                    // 【制御】検索:確認3
                    tmpRow = findControlCharacterRow(sheet, sheetname, "【制御】検索:確認3");
                    if (tmpRow != -1) {
                        sheet.getRow(tmpRow).getCell(col_value2).setCellValue(findKey);
                        // 制御文字を消す
                        clearControlCharacters(sheet, tmpRow, isTargetStrLeave);
                    }

                    // 【制御】検索:確認4
                    tmpRow = findControlCharacterRow(sheet, sheetname, "【制御】検索:確認4");
                    if (tmpRow != -1) {
                        sheet.getRow(tmpRow).getCell(col_value1).setCellValue(findKey);
                        // 制御文字を消す
                        clearControlCharacters(sheet, tmpRow, isTargetStrLeave);
                    }

                    // 【制御】新規:入力2
                    unique_index = 3;// ユニークインデックスの連番用
                    // 検索キー初期化
                    String findKey2 = findKey;
                    findKey = "";
                    findKey_columnname = "";
                    findKeyLevel = 0;
                    unique_index_Map.clear();
                    makeTestRow1(sheet, "【制御】新規:入力2", isTargetStrLeave, "ケース5", "準備(新規)");
                    String findKey3 = findKey;

                    // 【制御】検索:ケース5-1
                    tmpRow = findControlCharacterRow(sheet, sheetname, "【制御】検索:ケース5-1");
                    if (tmpRow != -1) {
                        sheet.getRow(tmpRow).getCell(col_value2).setCellValue(findKey2 + " " + findKey3);
                        // 制御文字を消す
                        clearControlCharacters(sheet, tmpRow, isTargetStrLeave);
                    }

                    // 【制御】検索:ケース5-2
                    tmpRow = findControlCharacterRow(sheet, sheetname, "【制御】検索:ケース5-2");
                    if (tmpRow != -1) {
                        sheet.getRow(tmpRow).getCell(col_value1).setCellValue(findKey2);
                        // 制御文字を消す
                        clearControlCharacters(sheet, tmpRow, isTargetStrLeave);
                    }

                    // 【制御】検索:ケース5-3
                    tmpRow = findControlCharacterRow(sheet, sheetname, "【制御】検索:ケース5-3");
                    if (tmpRow != -1) {
                        sheet.getRow(tmpRow).getCell(col_value1).setCellValue(findKey3);
                        // 制御文字を消す
                        clearControlCharacters(sheet, tmpRow, isTargetStrLeave);
                    }

                    // 【制御】新規:入力3
                    boolean is_unique_exist = false;
                    for (int i = 0; i < Kai9ComUtils.modelList.size(); i++) {
                        Models Models = Kai9ComUtils.modelList.get(i);
                        if (Models.unique_index.isEmpty())
                            continue;
                        is_unique_exist = true;
                        break;
                    }
                    if (is_unique_exist) {
                        // ユニークインデックスの指定があればシナリオを作成
                        unique_index = 2;// ユニークインデックスの連番用
                        // 検索キー初期化
                        findKey = "";
                        findKey_columnname = "";
                        findKeyLevel = 0;
                        // unique_index_Map.clear();クリアせず、前回のユニークキーを採用
                        makeTestRow1(sheet, "【制御】新規:入力3", isTargetStrLeave, "ケース6", "準備(新規)");
                    } else {
                        // ユニークインデックスの指定が無ければシナリオを行削除
                        List<Integer> rowsToDelete = new ArrayList<>();
                        tmpRow = 0;
                        for (Iterator<Row> rowIterator = sheet.iterator(); rowIterator.hasNext();) {
                            Row row = rowIterator.next();
                            tmpRow = row.getRowNum();
                            if (PoiUtil.GetStringValue(sheet, tmpRow, col_process_content).equals("ケース6")) {
                                rowsToDelete.add(tmpRow);// リストに記憶
                            }
                        }
                        // 記憶したリストを対象に削除
                        if (!rowsToDelete.isEmpty()) {
                            // 先頭1行だけはコピー用に残す
                            rowsToDelete.remove(0);
                            PoiUtil.removeRows(sheet, rowsToDelete);
                        }
                        TimeMeasurement.logTimeEnd("makeTestRow2:行削除", isDebug);

                        // TS2シートのユニークインデックスに関する記載に取り消し線を付ける
                        for (int i1 = 1; i1 < workbook.getNumberOfSheets(); i1++) {
                            Sheet ws = workbook.getSheetAt(i1);
                            if (!ws.getSheetName().equals("TS2"))
                                continue;

                            PoiUtil.addStrikethroughToCell(workbook, PoiUtil.findCell(ws, "ユニークインデックスの重複"));
                            PoiUtil.addStrikethroughToCell(workbook, PoiUtil.findCell(ws, "一意制約違反が発生する事を確認"));
                            PoiUtil.addStrikethroughToCell(workbook, PoiUtil.findCell(ws, "ナチュラルキーで、既存データが存在する場合のエラー"));
                            PoiUtil.addStrikethroughToCell(workbook, PoiUtil.findCell(ws, "一意制約違反"));

                            tmpRow = PoiUtil.findRow(ws, "ナチュラルキーで、既存データが存在する場合のエラー");
                            int tmpCol = PoiUtil.findCol(ws, tmpRow, "ナチュラルキーで、既存データが存在する場合のエラー");
                            PoiUtil.addStrikethroughToCell(workbook, ws.getRow(tmpRow).getCell(tmpCol - 2));
                        }
                    }

                }
                TimeMeasurement.logTimeEnd("処理シナリオ作成:「2」シート", isDebug);// 時間計測

                // シート「3」
                TimeMeasurement.logTimeStart("処理シナリオ作成:「3」シート", isDebug);// 時間計測
                if (sheet_roop_count == 3) {
                    // カラム単位のテスト明細作成
                    TimeMeasurement.logTimeStart("処理シナリオ作成:「3」シート：カラム単位のテスト明細作成", isDebug);// 時間計測
                    makeTestRow2(sheet, "【制御】新規:入力:必須", isTargetStrLeave);
                    makeTestRow2(sheet, "【制御】新規:入力:最小OK", isTargetStrLeave);
                    makeTestRow2(sheet, "【制御】新規:入力:最小NG", isTargetStrLeave);
                    makeTestRow2(sheet, "【制御】新規:入力:最大OK", isTargetStrLeave);
                    makeTestRow2(sheet, "【制御】新規:入力:最大NG", isTargetStrLeave);
                    makeTestRow2(sheet, "【制御】新規:入力:バリデーションOK", isTargetStrLeave);
                    makeTestRow2(sheet, "【制御】新規:入力:バリデーションNG", isTargetStrLeave);
                    TimeMeasurement.logTimeEnd("処理シナリオ作成:「3」シート：カラム単位のテスト明細作成", isDebug);// 時間計測
                }

                // 全ての計算式を再計算させる
                TimeMeasurement.logTimeStart("処理シナリオ作成:「3」シート：全ての計算式を再計算させる", isDebug);// 時間計測
                // FormulaEvaluatorを取得
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.FORMULA) {
                            try {
                                evaluator.evaluateFormulaCell(cell);
                            } catch (NotImplementedException e) {
                                // 計算式の反映に疑問があればコメントアウト解除し調査を。
                                // 今現在、エラーが出てるのはシート関数「'2'!G18」等の気にならない物ばかり。
                                // System.err.println("Formula in cell " + cell.getAddress() + " is not
                                // supported: " + e.getMessage());
                            }
                        }
                    }
                }
                TimeMeasurement.logTimeEnd("処理シナリオ作成:「3」シート：全ての計算式を再計算させる", isDebug);// 時間計測
                TimeMeasurement.logTimeEnd("処理シナリオ作成:「3」シート", isDebug);// 時間計測

            }
            // TS2、TS3シートの機能名を書き換える
            for (int i1 = 1; i1 < workbook.getNumberOfSheets(); i1++) {
                Sheet ws = workbook.getSheetAt(i1);
                if (!ws.getSheetName().equals("TS2") && !ws.getSheetName().equals("TS3"))
                    continue;
                int tmpRow = PoiUtil.findRow(ws, "シングル表");
                int tmpCol = PoiUtil.findCol(ws, tmpRow, "シングル表");
                ws.getRow(tmpRow).getCell(tmpCol).setCellValue(lTable_Name_J);
            }

            if (IsError) {
                // エラー表示して抜ける
                Kai9Util.ErrorMsg(String.join("", Errors));
                return;
            }
            TimeMeasurement.logTimeEnd("処理シナリオ作成", isDebug);// 時間計測

            // -------------------------------------------------------------------
            // 「2」シート:テストデータ等の作成
            // -------------------------------------------------------------------
            TimeMeasurement.logTimeStart("テストデータ作成", isDebug);// 時間計測
            // テストデータ原本をTMPフォルダへコピー
            File targetDir = null;
            if (!OurDir.endsWith("\\")) {
                targetDir = new File(OurDir + "\\テストデータ");
            } else {
                targetDir = new File(OurDir + "テストデータ");
            }
            FileUtils.copyDirectory(new File(m_env.getSvn_testdata_dir()), targetDir);
            // svnフォルダを削除
            FileUtils.deleteDirectory(new File(targetDir + "\\.svn"));

            // [TD12]同期sql_03
            // ファイル読み込み
            String filePath = targetDir + "\\同期sql_03\\001_DB初期設定1.sql";
            String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
            // 文字列を置換
            content = content.replace("kai9tmpl", projectName);
            // ファイル保存
            Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));

            // [TD12]同期sql_04
            // 既存ファイルを全て削除
            Path dirPath = Paths.get(targetDir + "\\同期sql_04");
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath)) {
                    for (Path delPath : directoryStream) {
                        if (Files.isRegularFile(delPath)) {
                            Files.delete(delPath);
                        }
                    }
                }
            }
            int index = 2;
            for (int i1 = 1; i1 < excel.getNumberOfSheets(); i1++) {
                Sheet ws = excel.getSheetAt(i1);
                // 制御文字が無いシートは無視
                int r1 = PoiUtil.findRow(ws, "#R1#");
                if (r1 == -1)
                    continue;
                int r2 = PoiUtil.findRow(ws, "#R2#");
                if (r2 == -1)
                    continue;
                int r3 = PoiUtil.findRow(ws, "#R3#");
                if (r3 == -1)
                    continue;
                int r4 = PoiUtil.findRow(ws, "#R4#");
                if (r4 == -1)
                    continue;
                int r5 = PoiUtil.findRow(ws, "#R5#");
                if (r5 == -1)
                    continue;
                int c_ddlA = PoiUtil.findCol(ws, r4 - 1, "DDL_A");
                if (c_ddlA == -1)
                    continue;
                int c_ddlB = PoiUtil.findCol(ws, r4 - 1, "DDL_B");
                if (c_ddlB == -1)
                    continue;
                int c_comA = PoiUtil.findCol(ws, r4, "コメント_A");
                if (c_comA == -1)
                    continue;
                int c_comB = PoiUtil.findCol(ws, r4, "コメント_B");
                if (c_comB == -1)
                    continue;
                int c_name = PoiUtil.findCol(ws, r1, "テーブル名(和名)");
                if (c_name == -1)
                    continue;

                // テーブル名(和名)
                String table_name = PoiUtil.GetStringValue(ws, r1, c_name + 2);

                StringBuilder sb = new StringBuilder();
                if (table_name.equals("DBバージョン")) {
                    // DBバージョンのSQL実行順を優先させる
                    filePath = targetDir + "\\同期sql_04\\" + String.format("%03d", 2) + "_" + table_name + ".sql";
                } else {
                    index++;
                    filePath = targetDir + "\\同期sql_04\\" + String.format("%03d", index) + "_" + table_name + ".sql";
                }

                // DDL_A
                int lastRow = PoiUtil.getLastRowNumInColumn(ws, c_ddlA);
                for (int r = r4; r <= lastRow; r++) {
                    sb.append(PoiUtil.GetStringValue(ws, r, c_ddlA)).append("\r\n");
                }
                sb.append("\r\n");

                // DDL_B
                lastRow = PoiUtil.getLastRowNumInColumn(ws, c_ddlB);
                for (int r = r4; r <= lastRow; r++) {
                    sb.append(PoiUtil.GetStringValue(ws, r, c_ddlB)).append("\r\n");
                }
                sb.append("\r\n");

                // コメント_A
                lastRow = PoiUtil.getLastRowNumInColumn(ws, c_comA);
                for (int r = r4 + 1; r <= lastRow; r++) {
                    sb.append(PoiUtil.GetStringValue(ws, r, c_comA)).append("\r\n");
                }
                sb.append("\r\n");

                // コメント_B
                lastRow = PoiUtil.getLastRowNumInColumn(ws, c_comB);
                for (int r = r4 + 1; r <= lastRow; r++) {
                    sb.append(PoiUtil.GetStringValue(ws, r, c_comB)).append("\r\n");
                }
                sb.append("\r\n");

                // 追加SQL
                int sqlRow = PoiUtil.findRow(ws, "LOGINユーザの権限");
                int sqlCol = PoiUtil.findCol(ws, sqlRow, "LOGINユーザの権限");
                if (sqlRow != -1 && sqlCol != -1) {
                    lastRow = PoiUtil.getLastRowNumInColumn(ws, sqlCol);
                    for (int r = sqlRow; r <= lastRow; r++) {
                        String s = PoiUtil.GetStringValue(ws, r, sqlCol);
                        if (s.contains("LOGINユーザの権限") || s.contains("シリアル型がある場合") || s.contains("ユニークインデックス")) {
                            // 各見出しに「--」を付与しコメント化、前後に改行も付与
                            sb.append("\r\n");
                            sb.append("--").append(s).append("\r\n");
                        } else if (s.toUpperCase().contains("GRANT") || s.toUpperCase().contains("CREATE")) {
                            // GRANT又はCREATEを含むSQLの場合だけ書き込む
                            sb.append(s).append("\r\n");
                        }
                    }
                }

                // テキストファイルに書き込む
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
                    writer.write(sb.toString());

                } catch (IOException e) {
                    // エラー表示して抜ける
                    Kai9Util.ErrorMsg(e.getMessage());
                    return;
                }
            }

            // 012_権限付与.sqlの作成
            StringBuilder sb = new StringBuilder();
            sb.append("--権限付与" + "\r\n");
            sb.append("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA " + projectName + " TO " + projectName + "admin;"
                    + "\r\n");
            sb.append("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA " + projectName + " TO "
                    + projectName + "pg;" + "\r\n");
            index++;
            filePath = targetDir + "\\同期sql_04\\" + String.format("%03d", index) + "_権限付与.sql";

            try (FileOutputStream fos = new FileOutputStream(filePath);
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
                // BOMを書き込む
                // fos.write(new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF});
                // ※BOMがあるとSQL読込時に、先頭の不可視の文字を感知し失敗してしまったので、付けない事にした
                writer.write(sb.toString());
            } catch (IOException e) {
                // エラー表示して抜ける
                Kai9Util.ErrorMsg(e.getMessage());
                return;
            }

            // ログイン用初期データ
            filePath = targetDir + "\\ログイン用初期データ\\m_user_a.xlsx";
            FileInputStream fis2 = new FileInputStream(filePath);
            Workbook wb = null;
            try {
                wb = WorkbookFactory.create(fis2);

                Sheet ws = wb.getSheetAt(0);

                // パスワードを書き込む
                int r1 = PoiUtil.findRow(ws, "#R1#");
                int r6 = PoiUtil.findRow(ws, "#R6#");
                if (r1 != -1 && r6 != -1) {
                    int c_pw = PoiUtil.findCol(ws, r1, "パスワード");
                    int c_note = PoiUtil.findCol(ws, r1, "備考");
                    // 不可逆エンコード
                    String encode_pw = passwordEncoder.encode(pw);
                    if (c_pw != -1) {
                        ws.getRow(r6).getCell(c_pw).setCellValue(encode_pw);
                    }

                    // 備考欄に暗号化用パスワードを格納(WEB画面ログイン用)
                    String encrypt_pw = encryptor.encrypt(pw);
                    encrypt_pw = "<暗号化>" + encrypt_pw + "</暗号化>";
                    if (c_note != -1) {
                        ws.getRow(r6).getCell(c_note).setCellValue(encrypt_pw);
                    }
                    // デバッグ用：パスワードを平分でログに記載
                    // ws.getRow(r6).getCell(c_pw + 6).setCellValue(pw);
                }

                // ファイルを上書き保存
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    wb.write(fos);
                }

            } catch (IOException e) {
                // エラー表示して抜ける
                Kai9Util.ErrorMsg(e.getMessage());
                return;
            } finally {
                if (wb != null) {
                    wb.close();
                }
                if (fis2 != null) {
                    fis2.close();
                }
            }
            TimeMeasurement.logTimeEnd("テストデータ作成", isDebug);// 時間計測

            // シート2だけ。Step列の計算式起因で何かしらの不具合が出て、ブックが壊れ修復メッセージが出てしまうため、計算式を最後に全て入れなおす事で回避
            sheet = workbook.getSheet("2"); // シート名を指定
            String tmpFormula = sheet.getRow(row3 + 1).getCell(col_step).getCellFormula();
            int lastRowNum = sheet.getLastRowNum();
            for (int i = row3 + 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell cell = row.getCell(col_step);
                if (cell == null) continue;

                // セルが数式を持っているか確認
                if (cell.getCellType() == CellType.FORMULA) {
                    if (cell.getCellFormula().equals(tmpFormula)) {
                        cell.setCellFormula(null);// ※これが大切。これが無いと修復になる。
                        cell.setCellFormula(tmpFormula);
                    }
                }
            }

            // 全シート1行目にスクロール
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet currentSheet = (XSSFSheet) workbook.getSheetAt(i);
                // セル選択
                currentSheet.setActiveCell(new CellAddress("A1"));

                // 「ウィンドウ枠の固定」の位置を記憶
                PaneInformation paneInfo = currentSheet.getPaneInformation();
                int frozenRow = (paneInfo != null) ? paneInfo.getHorizontalSplitPosition() : 0;
                int frozenCol = (paneInfo != null) ? paneInfo.getVerticalSplitPosition() : 0;

                // 一時的に「ウィンドウ枠の固定」を解除
                currentSheet.createFreezePane(0, 0);

                // トップセルを設定
                CTWorksheet ctWorksheet = currentSheet.getCTWorksheet();
                CTSheetViews ctSheetViews = ctWorksheet.getSheetViews();
                CTSheetView ctSheetView = ctSheetViews.getSheetViewArray(0);
                ctSheetView.setTopLeftCell("A1");

                // 「ウィンドウ枠の固定」を再設定
                if (frozenRow != 0 || frozenCol != 0) {
                    currentSheet.createFreezePane(frozenCol, frozenRow);
                }
            }
            // 一番左のシートをアクティブ状態にする
            workbook.setActiveSheet(0);
            // すべてのシートの選択状態を解除し、一番左のシートのみを選択状態にする
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet tmpSheet = (XSSFSheet) workbook.getSheetAt(i);
                if (i == 0) {
                    tmpSheet.setSelected(true);
                } else {
                    tmpSheet.setSelected(false);
                }
            }

            // calcChain.xmlの自動修復が走る場合があるので、シート削除でcalcChain.xmlが再計算される特性を生かして対策
            workbook.createSheet("新しいシート");
            workbook.removeSheetAt(workbook.getSheetIndex("新しいシート"));

            // エクセル保存
            TimeMeasurement.logTimeStart("エクセル保存", isDebug);// 時間計測
            Path saveDirectoryPath = Paths.get(OurDir, "処理シナリオ");
            Files.createDirectories(saveDirectoryPath);
            String saveFileName = Paths.get(saveDirectoryPath.toString(), "0000_処理シナリオ_" + lTable_Name_J + ".xlsx")
                    .toString();
            try (FileOutputStream fileOut = new FileOutputStream(saveFileName)) {
                workbook.write(fileOut);
                workbook = null;
            }
            TimeMeasurement.logTimeEnd("エクセル保存", isDebug);// 時間計測
        } catch (Exception e) {
            // 生成途中でアベンドした場合、デバッグ困難になるので、生成できた箇所までを保存する
            if (workbook != null) {
                // エクセル保存
                TimeMeasurement.logTimeStart("エクセル保存", isDebug);// 時間計測
                Path saveDirectoryPath = Paths.get(OurDir, "処理シナリオ");
                Files.createDirectories(saveDirectoryPath);
                String saveFileName = Paths.get(saveDirectoryPath.toString(), "0000_処理シナリオ_" + lTable_Name_J + ".xlsx")
                        .toString();
                try (FileOutputStream fileOut = new FileOutputStream(saveFileName)) {
                    workbook.write(fileOut);
                    Kai9Utils.makeLog("info", "アベンドしたのでデバッグ用エクセルを出力しました。" + saveFileName, this.getClass());
                    throw new IllegalArgumentException("アベンドしたのでデバッグ用エクセルを出力しました。" + saveFileName, e);
                }
            }

        } finally {
            if (workbook != null) {
                workbook.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
        TimeMeasurement.logPrint(isDebug);// 時間計測：結果表示

    }

    public int findControlCharacterRow(Sheet sheet, String sheetname, String ControlCharacter) {
        // 制御文字「#R1#」等の検索
        int row = PoiUtil.findRow(sheet, ControlCharacter);
        if (row == -1) {
            Errors.add("制御文字「" + ControlCharacter + "」がエクセルに発見できませんでした:シート名[" + sheetname + "]" + RN);
            IsError = true;
        }
        return row;
    }

    public int findControlCharacterCol(Sheet sheet, String sheetname, int pRow, String ControlCharacter) {
        // 制御文字「#C1#」等の検索
        int col = PoiUtil.findCol(sheet, pRow, ControlCharacter);
        if (col == -1) {
            Errors.add("制御文字「" + ControlCharacter + "」がエクセルに発見できませんでした:シート名[" + sheetname + "]" + RN);
            IsError = true;
        }
        return col;
    }

    public void setSellValue(Sheet sheet, String sheetname, int pRow, String FindColValue, int ColPlusIndex,
            String value) {
        int col = PoiUtil.findCol(sheet, pRow, FindColValue);
        if (col == -1) {
            Errors.add("制御文字「" + FindColValue + "」がエクセルに発見できませんでした:シート名[" + sheetname + "]" + RN);
            IsError = true;
        } else {
            Row row = sheet.getRow(pRow);
            Cell cell = row.getCell(col + ColPlusIndex);
            cell.setCellValue(value);
        }
    }

    // getNormalValueのサブルーチン。正常値を返すための文字列プール
    public static String getNormalValue(Models Models, LengthPattern LengthPattern, int unique_index) {
        String validationCheck = Models.validation_check;
        Faker faker = new Faker(new Locale("ja_JP"));
        FakeValuesService fakeValuesService = new FakeValuesService(new Locale("ja_JP"), new RandomService());

        // 出力する文字列の長さを決める
        int length = 0;
        switch (LengthPattern) {
        case MAX:
            length = Models.MaxLength;
            break;
        case MIN:
            length = Models.MinLength;
            break;
        case NORMAL:
            // ノーマル時は10文字にする
            length = 10;
            if (length > Models.MaxLength)
                length = Models.MaxLength;
            break;
        }
        if (length == 0)
            length = 10;// 最小や最大の指定が無い場合は10

        // 数値型でバリエーションが空の場合、数値限定として値を返す
        if (validationCheck.isEmpty() && Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
            validationCheck = "数値限定";
        }

        try {
            switch (validationCheck) {
            case "全角限定":
                return getFillValue(length, "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん");
            case "半角限定":
                return getFillValue(length,
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
            case "半角英字限定":
                return getFillValue(length, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
            case "半角数字限定":
                return getFillValue(length, "0123456789");
            case "半角記号限定":
                return getFillValue(length, "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
            case "半角カナ限定":
                return getFillValue(length, "ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝﾞﾟ");
            case "全角カナ限定":
                return getFillValue(length, "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン");

            case "小数点":
            case "数値限定":
                if (LengthPattern == ScenarioMaker.LengthPattern.MAX) {
                    if (Models.special_control_max != 0) {
                        // 特殊制御（最大数値
                        return decimalFormat.format(Models.special_control_max);
                    } else if (Models.data_type.toLowerCase().startsWith("numeric(")
                            || Models.data_type.toLowerCase().startsWith("decimal(")) {
                        // 精度指定型
                        int precision = Integer.parseInt(Models.data_type.substring(Models.data_type.indexOf('(') + 1,
                                Models.data_type.indexOf(',')));
                        int scale = Integer.parseInt(Models.data_type.substring(Models.data_type.indexOf(',') + 1,
                                Models.data_type.indexOf(')')));

                        // 最大値の文字列を生成する
                        StringBuilder maxValue = new StringBuilder();
                        for (int i = 0; i < precision - scale; i++) {
                            maxValue.append('9');
                        }
                        if (scale > 0) {
                            maxValue.append('.');
                            for (int i = 0; i < scale; i++) {
                                maxValue.append('9');
                            }
                        }
                        return maxValue.toString();
                    } else {
                        // その他、普通の数値型
                        String columnType = Models.data_type.toLowerCase();
                        if (columnType.contains("numeric")) {
                            columnType = "numeric";
                        } // データ型の簡略化
                        switch (columnType) {
                        case "smallserial":
                        case "smallint":
                            return String.valueOf(Short.MAX_VALUE); // 32767
                        case "integer":
                        case "serial":
                            return String.valueOf(Integer.MAX_VALUE); // 2147483647
                        case "bigint":
                        case "bigserial":
                            // postgresqlやjavaの精度より、tsx側の精度が低いので、低い方へ併せる
                            // return String.valueOf(Long.MAX_VALUE); // 9223372036854775807
                            return "9007199254740990";
                        case "real":
                            return String.format("%e", Float.MAX_VALUE); // 3.4028235E38
                        case "double precision":
                            return String.format("%e", Double.MAX_VALUE); // 1.7976931348623157E308
                        case "numeric":
                            return String.format("%e", new java.math.BigDecimal(Double.MAX_VALUE));// 1.7976931348623157E308
                        default:
                            return "Unsupported data type"; // サポートされていないデータ型の場合
                        }
                    }
                } else if (LengthPattern == ScenarioMaker.LengthPattern.MIN) {
                    if (Models.special_control_min != 0) {
                        // 特殊制御（最小数値
                        return decimalFormat.format(Models.special_control_min);
                    } else if (Models.data_type.toLowerCase().startsWith("numeric(")
                            || Models.data_type.toLowerCase().startsWith("decimal(")) {
                        // 精度指定型
                        int precision = Integer.parseInt(Models.data_type.substring(Models.data_type.indexOf('(') + 1,
                                Models.data_type.indexOf(',')));
                        int scale = Integer.parseInt(Models.data_type.substring(Models.data_type.indexOf(',') + 1,
                                Models.data_type.indexOf(')')));

                        // 最小値の文字列を生成する
                        StringBuilder minValue = new StringBuilder("-");
                        for (int i = 0; i < precision - scale; i++) {
                            minValue.append('9');
                        }
                        if (scale > 0) {
                            minValue.append('.');
                            for (int i = 0; i < scale; i++) {
                                minValue.append('9');
                            }
                        }
                        return minValue.toString();
                    } else {
                        // その他、普通の数値型
                        // 他の数値型の最小値を返す
                        String columnType = Models.data_type.toLowerCase();
                        if (columnType.contains("numeric")) {
                            columnType = "numeric";
                        } // データ型の簡略化
                        switch (columnType) {
                        case "smallserial":
                        case "smallint":
                            return String.valueOf(-Short.MAX_VALUE); // -32768
                        case "integer":
                        case "serial":
                            return String.valueOf(-Integer.MAX_VALUE); // -2147483648
                        case "bigint":
                        case "bigserial":
                            // postgresqlやjavaの精度より、tsx側の精度が低いので、低い方へ併せる
                            // return String.valueOf(Long.MIN_VALUE); // -9223372036854775808
                            return "-9007199254740990";
                        case "real":
                            return String.format("%e", (-Float.MAX_VALUE)); //  -3.4028235E38
                        case "double precision":
                            return String.format("%e", (-Double.MAX_VALUE)); // -1.7976931348623157E308
                        case "numeric":
                            return String.format("%e", (new java.math.BigDecimal(-Double.MAX_VALUE))); // - 1.7976931348623157E308
                        default:
                            return "unsupported data type"; // サポートされていないデータ型の場合
                        }
                    }
                } else if (LengthPattern == ScenarioMaker.LengthPattern.NORMAL) {
                    // 通常のデフォルト値を返す
                    // データ型が数値型（numericまたはdecimal）の場合に処理を実行
                    double return_value = 0;
                    if (Models.data_type.toLowerCase().startsWith("numeric(")
                            || Models.data_type.toLowerCase().startsWith("decimal(")) {
                        // データ型文字列からスケール値（小数点以下の桁数）を抽出
                        int scale = Integer.parseInt(Models.data_type.substring(Models.data_type.indexOf(',') + 1,
                                Models.data_type.indexOf(')')));
                        // デフォルト値を"1"から開始するStringBuilderを初期化
                        StringBuilder defaultValue = new StringBuilder(String.valueOf(unique_index));
                        // スケールが0より大きい場合、小数点とその後の0を追加
                        if (scale > 0) {
                            defaultValue.append('.');// 小数点を追加
                            for (int i = 0; i < scale; i++) {
                                defaultValue.append('0');// スケールの数だけ0を追加
                            }
                        }
                        // 組み立てたデフォルト値を文字列として返す（例："1.0", "1.00"等）
                        return_value = Double.parseDouble(defaultValue.toString());
                    } else {
                        return_value = unique_index; // 他の型でunique_indexをデフォルト値として返す
                    }

                    // 特殊制御（最小、最大数値 が有る場合、その範囲にとどめる
                    if (return_value < Models.special_control_min) {
                        return_value = Models.special_control_min;
                    } else if (return_value > Models.special_control_max) {
                        return_value = Models.special_control_max;
                    }

                    return decimalFormat.format(return_value);
                }

            case "郵便番号":
                if (unique_index <= 9) {
                    return String.format("%1$d%1$d%1$d-%1$d%1$d%1$d%1$d", unique_index, unique_index, unique_index,
                            unique_index);
                }
                return "000-0000";
            case "電話番号":
                if (unique_index <= 9) {
                    return String.format("%1$d%1$d%1$d-%1$d%1$d%1$d%1$d-%1$d%1$d%1$d%1$d", unique_index, unique_index,
                            unique_index, unique_index);
                }
                return "000-0000-0000";

            case "日付":
                if (LengthPattern == ScenarioMaker.LengthPattern.MAX) {
                    // 最大の場合、未来の日付を生成
                    Date futureDate = faker.date().future(1000, java.util.concurrent.TimeUnit.DAYS);
                    LocalDate futureLocalDate = futureDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return futureLocalDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                } else if (LengthPattern == ScenarioMaker.LengthPattern.MIN) {
                    // 最小の場合、過去の日付を生成
                    Date pastDate = faker.date().past(1000, java.util.concurrent.TimeUnit.DAYS);
                    LocalDate pastLocalDate = pastDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    return pastLocalDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                } else {
                    // ノーマルの場合、当日(+unique_index)
                    return LocalDate.now().plusDays(unique_index).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                }
            case "日時":
                if (LengthPattern == ScenarioMaker.LengthPattern.MAX) {
                    // 最大の場合、未来の日付を生成
                    Date futureDateTime = faker.date().future(1000, java.util.concurrent.TimeUnit.DAYS);
                    LocalDateTime futureLocalDateTime = futureDateTime.toInstant().atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    return futureLocalDateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                } else if (LengthPattern == ScenarioMaker.LengthPattern.MIN) {
                    // 最小の場合、過去の日付を生成
                    Date pastDateTime = faker.date().past(1000, java.util.concurrent.TimeUnit.DAYS);
                    LocalDateTime pastLocalDateTime = pastDateTime.toInstant().atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    return pastLocalDateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                } else {
                    // ノーマルの場合、当日(+unique_index)
                    return LocalDateTime.now().plusDays(unique_index)
                            .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                }
            case "メールアドレス":
                if (LengthPattern == ScenarioMaker.LengthPattern.NORMAL) {
                    // ノーマル時の10文字は、メールだと足りないので、100に変える
                    length = 100;
                }

                // 文字列の長さを調整
                StringBuilder pattern = new StringBuilder();
                for (int i = 0; i < length; i++) {
                    if (i % 2 == 0) {
                        pattern.append("?"); // 偶数位置に文字
                    } else {
                        pattern.append("#"); // 奇数位置に数字
                    }
                }
                String mail = faker.internet().safeEmailAddress(fakeValuesService.bothify(pattern.toString()));
                if (Models.MaxLength != 0) {
                    mail = mail.length() > length ? mail.substring(mail.length() - length) : mail;// 長ければ左をカット
                }
                if (Models.MinLength != 0) {
                    mail = String.format("%" + length + "s", mail).replace(' ', 'X');// 短ければxで埋める
                }
                return mail;
            case "URL":
                // ドメイン名とTLDを生成する
                String domainName = faker.internet().domainName();
                String tld = faker.internet().domainSuffix();
                // ドメイン名をIDN形式にエンコードする
                String encodedDomainName = IDN.toASCII(domainName);
                // ドメイン名の長さを調整する
                if (Models.MinLength != 0) {
                    encodedDomainName = String.format("%-" + Models.MinLength + "s", encodedDomainName).replace(' ',
                            'X'); // 短ければ末尾をXで埋める
                }
                if (Models.MaxLength != 0) {
                    encodedDomainName = encodedDomainName.length() > Models.MaxLength
                            ? encodedDomainName.substring(0, Models.MaxLength)
                            : encodedDomainName; // 長ければ右をカット
                }
                // 完全なURLの形にする
                String url = "https://" + encodedDomainName + "." + tld;
                return url;
            default:
                // "正規表現" が含まれている場合の処理
                if (validationCheck.contains("【正規表現】")) {
                    // "正規表現" 部分を削除して正規表現パターンを抽出
                    String regex = validationCheck.replace("【正規表現】", "").trim();
                    regex = regex.replace("^", "").replace("$", ""); // ^と$を取り除く

                    // 正規表現の場合、最大、最小の文字列生成は仕様上厳しい(正規表現自体に文字数を制御するものがある)ので、そのまま返す
                    return fakeValuesService.regexify(regex);
                }

                // セレクトボックス
                if (Models.input_type.toLowerCase().equals("selectinput")) {
                    // セレクトボックスの場合1番目の要素を選択する。【1】で1番目になる仕様。
                    return "【" + String.valueOf(unique_index) + "】";
                }

                // ユニークインデックス
                if (!Models.unique_index.isEmpty()) {

                    // 数値を連番で返す
                    String str = String.valueOf(unique_index);
                    if (LengthPattern == kai9.com.srcmake.ScenarioMaker.LengthPattern.MAX
                            || LengthPattern == kai9.com.srcmake.ScenarioMaker.LengthPattern.MIN) {
                        // 最小、最大になるまで1文字目を加算
                        int len = length - str.length();
                        str = str.isEmpty() ? str : String.valueOf(str.charAt(0)).repeat(len) + str;
                    } else if (LengthPattern == kai9.com.srcmake.ScenarioMaker.LengthPattern.NORMAL && Models.MinLength != 0) {
                        // ノーマルで最小が決まっている場合、最小の文字数まで1文字目を加算
                        int len = Models.MinLength - str.length();
                        str = str.isEmpty() ? str : String.valueOf(str.charAt(0)).repeat(len) + str;
                    }
                    return str;

                    // ①②③等の複数のユニークインデックスが組み合わさったとしても、、1レコード毎に、それら全てに1から連番で入れる限り、重複しない（と思う）
                    // 文字か数値か判らないが、どちらにせよ、数値なら入るし、桁数も最小になるので、数値を入れる仕様とする
                }

                // バリデーション無し、又は、正体不明のバリデーション
                return String.valueOf(unique_index);
            }
        } catch (Exception e) {
            return "生成失敗:" + e.getMessage();
        }
    }

    // getAbnormalValueのサブルーチン。異常値を返すための文字列プール
    private String getAbnormalValue(Models Models, LengthPattern LengthPattern, int unique_index) {
        String validationCheck = Models.validation_check;
        Faker faker = new Faker(new Locale("ja_JP"));

        // 出力する文字列の長さを決める
        int length = 0;
        switch (LengthPattern) {
        case MAX:
            length = Models.MaxLength + 1;// +1
            break;
        case MIN:
            length = Models.MinLength - 1;// -1
            break;
        case NORMAL:
            // ノーマル時は10文字にする
            length = 10;
            if (length > Models.MaxLength)
                length = Models.MaxLength;
            break;
        }
        if (length == 0)
            length = 10;// 最小や最大の指定が無い場合は10

        // 数値型でバリエーションが空の場合、数値限定として値を返す
        if (validationCheck.isEmpty() && Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
            validationCheck = "数値限定";
        }

        try {
            switch (validationCheck) {
            // 異常系なので、各バリエーションの文字列テーブルを入れ替えて返している(例: 全角は半角 など
            case "全角限定":
                return getFillValue(length,
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
            case "半角限定":
                return getFillValue(length, "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン");
            case "半角英字限定":
                return getFillValue(length, "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん");
            case "半角数字限定":
                return getFillValue(length, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
            case "半角記号限定":
                return getFillValue(length, "ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝﾞﾟ");
            case "半角カナ限定":
                return getFillValue(length, "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
            case "全角カナ限定":
                return getFillValue(length, "0123456789");

            case "小数点":
            case "数値限定":
                if (LengthPattern == ScenarioMaker.LengthPattern.MAX) {
                    if (Models.special_control_max != 0) {
                        // 特殊制御（最小数値
                        return String.valueOf(Models.special_control_max + 1);// +1
                    } else if (Models.data_type.toLowerCase().startsWith("numeric(")
                            || Models.data_type.toLowerCase().startsWith("decimal(")) {
                        // データ型文字列から精度とスケールを抽出する
                        // 精度 (precision) は全体の桁数を示し、スケール (scale) は小数点以下の桁数を示す。例）numeric(5,2)の最大は999.99
                        int precision = Integer.parseInt(Models.data_type.substring(Models.data_type.indexOf('(') + 1,
                                Models.data_type.indexOf(',')));
                        int scale = Integer.parseInt(Models.data_type.substring(Models.data_type.indexOf(',') + 1,
                                Models.data_type.indexOf(')')));

                        // 精度とスケールに基づいて最大値の文字列を生成する
                        StringBuilder maxValue = new StringBuilder();
                        // 精度 - スケール の数だけ '9' を追加する
                        for (int i = 0; i < precision - scale; i++) {
                            maxValue.append('9');
                        }
                        // スケールが0より大きい場合、小数点を追加し、その後スケールの数だけ '9' を追加する
                        if (scale > 0) {
                            maxValue.append('.');
                            for (int i = 0; i < scale; i++) {
                                maxValue.append('9');
                            }
                        }
                        return maxValue.toString() + "9";// 最大値に1桁追加する
                    } else {
                        // 最大値に+1する
                        String columnType = Models.data_type.toLowerCase();
                        if (columnType.contains("numeric")) {
                            columnType = "numeric";
                        } // データ型の簡略化
                        switch (columnType) {
                        case "smallserial":
                        case "smallint":
                            return String.valueOf((long) Short.MAX_VALUE + 1); // 32768
                        case "integer":
                        case "serial":
                            return String.valueOf((long) Integer.MAX_VALUE + 1); // 2147483648
                        case "bigint":
                        case "bigserial":
                            // bigintの最大値に1を加えるとオーバーフローするため、特別な処理が必要
                            // return new java.math.BigDecimal(Long.MAX_VALUE).add(java.math.BigDecimal.ONE).toString(); // 9223372036854775808
                            // postgresqlやjavaの精度より、tsx側の精度が低いので、低い方へ併せる
                            return "9007199254740991";

                        case "real":
                            return String.format("%e", (Float.MAX_VALUE + 1)); // 3.4028235E38 + 1
                        case "double precision":
                            return String.format("%e", (Double.MAX_VALUE + 1)); // 1.7976931348623157E308 + 1
                        default:
                            return "unsupported data type"; // サポートされていないデータ型の場合
                        }
                    }
                } else if (LengthPattern == ScenarioMaker.LengthPattern.MIN) {
                    if (Models.special_control_min != 0) {
                        // 特殊制御（最小数値
                        return String.valueOf(Models.special_control_min - 1);// -1
                    } else if (Models.data_type.toLowerCase().startsWith("numeric(")
                            || Models.data_type.toLowerCase().startsWith("decimal(")) {
                        int precision = Integer.parseInt(Models.data_type.substring(Models.data_type.indexOf('(') + 1,
                                Models.data_type.indexOf(',')));
                        int scale = Integer.parseInt(Models.data_type.substring(Models.data_type.indexOf(',') + 1,
                                Models.data_type.indexOf(')')));

                        // 最小値の文字列を生成する
                        StringBuilder minValue = new StringBuilder("-");
                        for (int i = 0; i < precision - scale; i++) {
                            minValue.append('9');
                        }
                        if (scale > 0) {
                            minValue.append('.');
                            for (int i = 0; i < scale; i++) {
                                minValue.append('9');
                            }
                        }
                        return minValue.toString() + '9';// +1桁
                    } else {
                        // 他の数値型の最小値を返す
                        String columnType = Models.data_type.toLowerCase();
                        if (columnType.contains("numeric")) {
                            columnType = "numeric";
                        } // データ型の簡略化
                        switch (columnType) {
                        case "smallserial":
                        case "smallint":
                            return String.valueOf((long) -Short.MAX_VALUE - 1); // -32769
                        case "integer":
                        case "serial":
                            return String.valueOf((long) -Integer.MAX_VALUE - 1); // -2147483649
                        case "bigint":
                        case "bigserial":
                            // bigintの最小値から-1を引くとオーバーフローするため、特別な処理が必要
                            // return new java.math.BigDecimal(Long.MIN_VALUE).subtract(java.math.BigDecimal.ONE)
                            // .toString(); // -9223372036854775809
                            // postgresqlやjavaの精度より、tsx側の精度が低いので、低い方へ併せる
                            return "-9007199254740991";
                        case "real":
                            return String.format("%e", (-Float.MAX_VALUE - 1)); // 3.4028235E38 - 1
                                                                               // (最小の正の非ゼロ値から1を引くため、浮動小数点の特性上結果は-1.0になります)
                        case "double precision":
                            return String.format("%e", (-Double.MAX_VALUE - 1)); // 1.7976931348623157E308 - 1
                                                                                // (同様に、浮動小数点の特性上結果は-1.0になります)
                        default:
                            return "unsupported data type"; // サポートされていないデータ型の場合
                        }
                    }
                } else if (LengthPattern == ScenarioMaker.LengthPattern.NORMAL) {
                    // 数値ノーマルの異常値は作れない。「あ」等が、そもそも入らないので。
                    return String.valueOf(0);
                }

            case "郵便番号":
                if (LengthPattern == kai9.com.srcmake.ScenarioMaker.LengthPattern.NORMAL) {
                    return "abnormal";
                }
                if (LengthPattern == kai9.com.srcmake.ScenarioMaker.LengthPattern.MAX) {
                    if (unique_index <= 9) {
                        return String.format("%1$d%1$d%1$d-%1$d%1$d%1$d%1$d-%1$d%1$d%1$d%1$d", unique_index,
                                unique_index, unique_index, unique_index);
                    }
                }
                return "0-0-0";// 最小のエラーは出ない
            case "電話番号":
                if (unique_index <= 9) {
                    return String.format("%1$d%1$d%1$d-%1$d%1$d%1$d%1$d", unique_index, unique_index, unique_index,
                            unique_index);
                }
                return "000-0000";

            case "日付":
                return "9999/99/99";
            case "日時":
                return "9999/99/99 99:99:99";
            case "メールアドレス":
                String returnStr = "abnormal-mail";
                if (LengthPattern != kai9.com.srcmake.ScenarioMaker.LengthPattern.NORMAL) {
                    returnStr = getNormalValue(Models, LengthPattern, unique_index);
                }
                // 長すぎる場合は左端からカットし、短すぎる場合は左端の1文字を増幅
                return returnStr.length() > length ? returnStr.substring(0, length)
                        : returnStr + String.valueOf(returnStr.charAt(0)).repeat(length - returnStr.length());
            case "URL":
                returnStr = "abnormal-url";
                if (LengthPattern != kai9.com.srcmake.ScenarioMaker.LengthPattern.NORMAL) {
                    returnStr = getNormalValue(Models, LengthPattern, unique_index);
                }
                // 長すぎる場合は左端からカットし、短すぎる場合は左端の1文字を増幅
                return returnStr.length() > length ? returnStr.substring(0, length)
                        : returnStr + String.valueOf(returnStr.charAt(0)).repeat(length - returnStr.length());

            default:
                // "正規表現" が含まれている場合の処理
                if (validationCheck.contains("【正規表現】")) {

                    // "正規表現" 部分を削除して正規表現パターンを抽出
                    String regex = validationCheck.replace("【正規表現】", "").trim();
                    regex = regex.replace("^", "").replace("$", ""); // ^と$を取り除く

                    int minLength = (Models.MinLength == 0) ? 10 : Models.MinLength;
                    int maxLength = (Models.MaxLength == 0) ? 20 : Models.MaxLength;

                    // 正規表現にマッチしない文字列のランダム生成を100回行う
                    for (int i = 0; i < 100; i++) {
                        // ランダムな文字列を生成
                        String randomString = faker.lorem().characters(minLength, maxLength); // 10から20文字のランダムな文字列を生成

                        // 正規表現にマッチしない場合、それを返す
                        if (!randomString.matches(regex)) {
                            return randomString;
                        }
                    }
                    return "正規表現(自動生成不可能)";
                }

                // セレクトボックス
                if (Models.input_type.toLowerCase().equals("selectinput")) {
                    // セレクトボックスの場合1番目の要素を選択する。【1】で1番目になる仕様。
                    // 異常系は作成できないので正常系を返す
                    return "【" + String.valueOf(unique_index) + "】";
                }

                // ユニークインデックス
                if (!Models.unique_index.isEmpty()) {
                    // 数値を連番で返す
                    // 異常系は返せないので正常系で返す

                    // 数値を連番で返す
                    String str = String.valueOf(unique_index);
                    str = str.isEmpty() ? str : String.valueOf(str.charAt(0)).repeat(length - str.length()) + str;
                    if (LengthPattern == kai9.com.srcmake.ScenarioMaker.LengthPattern.NORMAL && Models.MinLength != 0) {
                        // ノーマルで最小が決まっている場合、
                        // 最小から1文字数減らした文字数になるまで1文字目を加算
                        int len = Models.MinLength - str.length();
                        str = str.isEmpty() ? str : String.valueOf(str.charAt(0)).repeat(len - 1) + str;
                    }
                    return str;
                }

                // 正体不明のバリデーション
                returnStr = "バリデーション無し" + unique_index;
                // 長すぎる場合は右端からカットし、短すぎる場合は右端の1文字を増幅
                return returnStr.length() > length ? returnStr.substring(0, length)
                        : returnStr + String.valueOf(returnStr.charAt(returnStr.length() - 1))
                                .repeat(length - returnStr.length());
            }
        } catch (Exception e) {
            return "生成失敗:" + e.getMessage();
        }
    }

    public static String getFillValue(int MaxLength, String value) {
        // 結果を格納するStringBuilderを初期化
        StringBuilder sb = new StringBuilder(MaxLength);

        // 文字プールの長さを取得
        int charPoolLength = value.length();

        // ランダムに文字を選定する
        Random random = new Random();

        // 指定された最小長に達するまで文字プールを繰り返し追加
        for (int i = 0; i < MaxLength; i++) {
            sb.append(value.charAt(random.nextInt(charPoolLength)));
        }

        // 生成された文字列を返す
        return sb.toString();
    }

    public static void makeTestRow1(Sheet sheet, String targetValue, boolean isTargetStrLeave, String process_content,
            String comment) {
        // 制御対象の行を削除
        List<Integer> rowsToDelete = new ArrayList<>();
        int tmpRow = 0;
        int targetRowIndex = 0;
        for (Iterator<Row> rowIterator = sheet.iterator(); rowIterator.hasNext();) {
            Row row = rowIterator.next();
            tmpRow = row.getRowNum();
            if (PoiUtil.GetStringValue(sheet, tmpRow, 0).equals(targetValue)) {
                rowsToDelete.add(tmpRow);// リストに記憶
                if (targetRowIndex == 0) {
                    targetRowIndex = tmpRow; // 初回削除行を記憶
                }
            }
        }
        if (targetRowIndex == 0) {
            Errors.add("制御文字が存在しません: 制御文字=" + targetValue + RN);
            Kai9Util.ErrorMsg(String.join("", Errors));
            return;
        }
        // 記憶したリストを対象に削除
        if (!rowsToDelete.isEmpty() && rowsToDelete.size() != 1) {
            // 先頭1行だけはコピー用に残す
            rowsToDelete.remove(0);
            PoiUtil.removeRows(sheet, rowsToDelete);
        }
        TimeMeasurement.logTimeEnd("makeTestRow2:行削除", isDebug);

        // 制御対象の行を追加
        boolean isFirst = true;
        for (int i = Kai9ComUtils.modelList.size() - 1; i >= 0; i--) {// 降順ループ
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.is_pk)
                continue;
            if (Kai9ComUtils.isSystemColumn(Models.columnname))
                continue;

            // 新しい行を挿入する
            if (!isFirst) {
                try {
                    PoiUtil.insertRow(sheet, targetRowIndex);
                } catch (Exception e) {
                    throw new IllegalArgumentException("point3:" + targetValue);
                }
                // 新しい行の書式をコピーする
                if (targetRowIndex > 0) {
                    PoiUtil.copyRowFormatting(sheet, targetRowIndex - 1, targetRowIndex);
                }
            } else {
                // 1行目は雛形なのでコピーせず、そのまま使う
                isFirst = false;
            }
            // 各値をセット
            sheet.getRow(targetRowIndex).getCell(col_process_content).setCellValue(process_content);
            sheet.getRow(targetRowIndex).getCell(col_comment).setCellValue(comment);
            sheet.getRow(targetRowIndex).getCell(col_total_count).setCellValue("1");
            sheet.getRow(targetRowIndex).getCell(col_value1).setCellValue(Models.columnname);
            sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("OK");
            if (Models.input_type.toUpperCase().equals("TEXTINPUT")
                    || Models.input_type.toUpperCase().equals("TEXTAREA")
                    || Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
                // テキスト系
                sheet.getRow(targetRowIndex).getCell(col_keyword).setCellValue("[web]テキスト入力(id)");
                String str = getNormalValue(Models, LengthPattern.NORMAL, unique_index);
                sheet.getRow(targetRowIndex).getCell(col_value2).setCellValue(str);
            }
            if (Models.input_type.toUpperCase().equals("CHECKBOX")) {
                // チェックボックス
                sheet.getRow(targetRowIndex).getCell(col_keyword).setCellValue("[web]チェックボックス選択(id)");
                sheet.getRow(targetRowIndex).getCell(col_value2).setCellValue("FALSE");
            }

            if (Models.input_type.toUpperCase().equals("SELECTINPUT")) {
                // セレクト
                sheet.getRow(targetRowIndex).getCell(col_keyword).setCellValue("[web]セレクトボックス選択(id)");
                String str = getNormalValue(Models, LengthPattern.NORMAL, unique_index);
                sheet.getRow(targetRowIndex).getCell(col_value2).setCellValue(str);
            }
            // 指定があれば、制御文字を残す
            if (isTargetStrLeave) {
                if (sheet.getRow(targetRowIndex).getCell(0) == null) {
                    sheet.getRow(targetRowIndex).createCell(0);// 空セルの場合はセルを作成
                }
                sheet.getRow(targetRowIndex).getCell(0).setCellValue(targetValue);
            }

            // 検索キーを確保 （何れも該当しない場合、空文字で検索させる）
            String tmpKey = sheet.getRow(targetRowIndex).getCell(col_value2).getStringCellValue().trim();

            // ユニークキー
            if (!Models.unique_index.isEmpty()) {
                if (unique_index_Map.containsKey(Models.columnname)) {
                    // 既にセット済のユニークキーがある場合、一意制約違反用の値なので、それを採用する
                    tmpKey = unique_index_Map.get(Models.columnname);
                    sheet.getRow(targetRowIndex).getCell(col_value2).setCellValue(tmpKey);
                } else {
                    // ユニークキーを確保
                    unique_index_Map.put(Models.columnname, tmpKey);
                }
            }

            // 特殊制御を除き、findkeyを確保する
            // booleanも除く(表記時にFALSEが空欄になったり特殊なので)
            if (Models.special_control_relation.isEmpty() && !Models.data_type.toLowerCase().equals("boolean")) {
                // varchar、char 長い文字列があれば優先
                // 条件1: データタイプが文字列の場合、長い文字列を優先
                if (Models.data_type.toLowerCase().contains("char") && findKeyLevel < tmpKey.length()) {
                    findKeyLevel = tmpKey.length();
                    findKey = tmpKey;
                    findKey_columnname = Models.columnname;
                }

                // ユニークキー
                // 条件2: ユニークキーが存在し、現在の優先度が5未満の場合、優先度を10に設定
                if (!Models.unique_index.isEmpty() && findKeyLevel < 5) {
                    findKeyLevel = 10;// 最優先
                    findKey = tmpKey;
                    findKey_columnname = Models.columnname;
                }

                // NumberInput
                // 条件3: 入力タイプがNumberInputで、現在の優先度が1未満の場合、優先度を1に設定
                if (Models.input_type.toLowerCase().equals("numberinput") && findKeyLevel < 1) {
                    findKeyLevel = 1;// 最低の優先順
                    findKey = tmpKey;
                    findKey_columnname = Models.columnname;
                }
            }
        }
    }

    public void makeTestRow2(Sheet sheet, String targetValue, boolean isTargetStrLeave) {
        // 制御対象の行を削除
        List<Integer> rowsToDelete = new ArrayList<>();
        int tmpRow = 0;
        int targetRowIndex = 0;
        int removeRowsFirst = 0;
        for (Iterator<Row> rowIterator = sheet.iterator(); rowIterator.hasNext();) {
            Row row = rowIterator.next();
            tmpRow = row.getRowNum();
            if (PoiUtil.GetStringValue(sheet, tmpRow, 0).equals(targetValue)) {
                rowsToDelete.add(tmpRow);// リストに記憶
                if (targetRowIndex == 0) {
                    targetRowIndex = tmpRow; // 初回削除行を記憶
                }
            }
        }

        // 記憶したリストを対象に削除
        TimeMeasurement.logTimeStart("makeTestRow2:行削除", isDebug);
        if (!rowsToDelete.isEmpty()) {
            // 先頭1行だけはコピー用に残す
            removeRowsFirst = rowsToDelete.get(0);
            rowsToDelete.remove(0);
            PoiUtil.removeRows(sheet, rowsToDelete);
        }
        TimeMeasurement.logTimeEnd("makeTestRow2:行削除", isDebug);

        // 制御対象の行を追加
        boolean isFirst = true;
        boolean isExist = false;
        for (int i = Kai9ComUtils.modelList.size() - 1; i >= 0; i--) {// 降順ループ
            Models Models = Kai9ComUtils.modelList.get(i);
            if (Models.is_pk) continue;
            if (Kai9ComUtils.isSystemColumn(Models.columnname)) continue;
            // リレーションはバリデーションテスト不要なので無視
            if (!Models.special_control_relation.isEmpty()) continue;

            // 必須入力の判定
            boolean isRequiredInput = false;
            if (targetValue.equals("【制御】新規:入力:必須")) {
                if (Models.is_not_null) {
                    // 必須入力が設定されている場合
                    isRequiredInput = true;
                }
            }

            // 最小OK入力の判定
            boolean isMinOkInput = false;
            if (targetValue.equals("【制御】新規:入力:最小OK")) {
                if (Models.input_type.toUpperCase().equals("NUMBERINPUT") || Models.MinLength != 0) {
                    // 入力タイプがNUMBERINPUTまたは最小長さが0でない場合
                    if (!Models.validation_check.equals("電話番号") && !Models.validation_check.equals("郵便番号")) {
                        // バリデーションチェックが電話番号または郵便番号でない場合
                        isMinOkInput = true;
                    }
                }
            }

            // 最小NG入力の判定
            boolean isMinNgInput = false;
            if (targetValue.equals("【制御】新規:入力:最小NG")) {
                if (Models.input_type.toUpperCase().equals("NUMBERINPUT") || Models.MinLength >= 2) {
                    // 入力タイプがNUMBERINPUTまたは最小長さが2以上の場合
                    if (!Models.validation_check.equals("電話番号") && !Models.validation_check.equals("郵便番号")) {
                        // バリデーションチェックが電話番号または郵便番号でない場合
                        isMinNgInput = true;
                    }
                }
            }

            // 最大OK入力の判定
            boolean isMaxOkInput = false;
            if (targetValue.equals("【制御】新規:入力:最大OK")) {
                if (Models.input_type.toUpperCase().equals("NUMBERINPUT") || Models.MaxLength != 0) {
                    // 入力タイプがNUMBERINPUTまたは最大長さが0でない場合
                    if (!Models.validation_check.equals("電話番号") && !Models.validation_check.equals("郵便番号")) {
                        // バリデーションチェックが電話番号または郵便番号でない場合
                        isMaxOkInput = true;
                    }
                }
            }

            // 最大NG入力の判定
            boolean isMaxNgInput = false;
            if (targetValue.equals("【制御】新規:入力:最大NG")) {
                if (Models.input_type.toUpperCase().equals("NUMBERINPUT") || Models.MaxLength >= 2) {
                    // 入力タイプがNUMBERINPUTまたは最大長さが2以上の場合
                    if (!Models.validation_check.equals("電話番号") && !Models.validation_check.equals("郵便番号")) {
                        // バリデーションチェックが電話番号または郵便番号でない場合
                        isMaxNgInput = true;
                    }
                }
            }

            // バリデーションOKの判定
            boolean isValidationOkInput = false;
            if (targetValue.equals("【制御】新規:入力:バリデーションOK")) {
                if (!Models.validation_check.isEmpty()) {
                    // バリデーションチェックが空でない場合
                    isValidationOkInput = true;
                }
            }

            // バリデーションNGの判定
            boolean isValidationNgInput = false;
            if (targetValue.equals("【制御】新規:入力:バリデーションNG")) {
                if (!Models.validation_check.isEmpty()) {
                    // 数値と小数点を除く(NG値が再現できないので)
                    if (!Models.validation_check.equals("数値限定") && !Models.validation_check.equals("小数点")) {
                        // 日付と日時を除く(NG値が再現できないので)
                        if (!Models.validation_check.equals("日付") && !Models.validation_check.equals("日時")) {
                            // バリデーションチェックが空でない場合
                            isValidationNgInput = true;
                        }
                    }
                }
            }

            // 総合判定
            if (isRequiredInput || isMinOkInput || isMinNgInput || isMaxOkInput || isMaxNgInput || isValidationOkInput
                    || isValidationNgInput) {

                isExist = true;
            } else {
                // どのパターンにもマッチしない場合はテストデータを作成しない
                continue;
            }

            // 必要な行数分をセットでコピー
            TimeMeasurement.logTimeStart("makeTestRow2:行追加", isDebug);
            for (int copy_i = 1; copy_i <= 3; copy_i++) {
                // OK以外は3行目(1ループ目)不要
                if (isMinOkInput || isMaxOkInput || isValidationOkInput) {
                } else {
                    if (copy_i == 1)
                        continue;
                }

                if (!isFirst) {
                    // 新しい行を挿入する
                    TimeMeasurement.logTimeStart("makeTestRow2:行追加:行挿入", isDebug);
                    PoiUtil.insertRow(sheet, targetRowIndex);
                    TimeMeasurement.logTimeEnd("makeTestRow2:行追加:行挿入", isDebug);
                    // 新しい行の書式をコピーする
                    if (targetRowIndex > 0) {
                        PoiUtil.copyRowFormatting(sheet, targetRowIndex + 1, targetRowIndex);
                    }
                } else {
                    // 1行目は雛形なのでコピーせず、そのまま使う
                    isFirst = false;
                }

                // 各値をセット
                sheet.getRow(targetRowIndex).getCell(col_total_count).setCellValue("1");
                if (copy_i == 1) {
                    sheet.getRow(targetRowIndex).getCell(col_value1).setCellFormula("INDIRECT(\"B\" & ROW() - 1)");
                    sheet.getRow(targetRowIndex).getCell(col_value2).setCellValue("エレメントを取得できませんでした");
                } else if (copy_i == 2) {
                    sheet.getRow(targetRowIndex).getCell(col_value1).setCellFormula(null); // 計算式のキャッシュクリア(poiのバグ？でこれをしないと計算式が残り、値が入らない)
                    sheet.getRow(targetRowIndex).getCell(col_value1).setCellValue("invalid_" + Models.columnname);
                } else if (copy_i == 3) {
                    sheet.getRow(targetRowIndex).getCell(col_value1).setCellFormula(null);
                    sheet.getRow(targetRowIndex).getCell(col_value1).setCellValue(Models.columnname);
                }
                sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("");// 初期化

                // 指定があれば、制御文字を残す
                if (isTargetStrLeave) {
                    if (sheet.getRow(targetRowIndex).getCell(0) == null) {
                        sheet.getRow(targetRowIndex).createCell(0);// 空セルの場合はセルを作成
                    }
                    sheet.getRow(targetRowIndex).getCell(0).setCellValue(targetValue);
                }

                // 各値をセット
                if (isRequiredInput) {
                    // 必須
                    sheet.getRow(targetRowIndex).getCell(col_process_content).setCellValue("ケース1");
                    sheet.getRow(targetRowIndex).getCell(col_comment).setCellValue("必須");
                    sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("OK");
                    if (copy_i == 2) {
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("含まれる");
                    }
                } else if (isMinOkInput) {
                    // 最小OK
                    sheet.getRow(targetRowIndex).getCell(col_process_content).setCellValue("ケース2");
                    sheet.getRow(targetRowIndex).getCell(col_comment).setCellValue("最小OK");
                    if (copy_i == 1) {
                        sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("一致有り");
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("");
                    } else if (copy_i == 2) {
                        sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("NG");
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("含まれる,1");
                    } else if (copy_i == 3) {
                        sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("OK");
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("");
                    }
                } else if (isMinNgInput) {
                    // 最小NG
                    sheet.getRow(targetRowIndex).getCell(col_process_content).setCellValue("ケース3");
                    sheet.getRow(targetRowIndex).getCell(col_comment).setCellValue("最小NG");
                    sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("OK");
                    if (copy_i == 2) {
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("完全一致");
                    }
                } else if (isMaxOkInput) {
                    // 最大OK
                    sheet.getRow(targetRowIndex).getCell(col_process_content).setCellValue("ケース4");
                    sheet.getRow(targetRowIndex).getCell(col_comment).setCellValue("最大OK");
                    if (copy_i == 1) {
                        sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("一致有り");
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("");
                    } else if (copy_i == 2) {
                        sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("NG");
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("含まれる,1");
                    } else if (copy_i == 3) {
                        sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("OK");
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("");
                    }
                } else if (isMaxNgInput) {
                    // 最大NG
                    sheet.getRow(targetRowIndex).getCell(col_process_content).setCellValue("ケース5");
                    sheet.getRow(targetRowIndex).getCell(col_comment).setCellValue("最大NG");
                    sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("OK");
                    if (copy_i == 2) {
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("完全一致");
                    }
                } else if (isValidationOkInput) {
                    // バリデーションOK
                    sheet.getRow(targetRowIndex).getCell(col_process_content).setCellValue("ケース6");
                    sheet.getRow(targetRowIndex).getCell(col_comment).setCellValue("バリデーションOK");
                    if (copy_i == 1) {
                        sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("一致有り");
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("");
                    } else if (copy_i == 2) {
                        sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("NG");
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("含まれる,1");
                    } else if (copy_i == 3) {
                        sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("OK");
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("");
                    }
                } else if (isValidationNgInput) {
                    // バリデーションNG
                    sheet.getRow(targetRowIndex).getCell(col_process_content).setCellValue("ケース7");
                    sheet.getRow(targetRowIndex).getCell(col_comment).setCellValue("バリデーションNG");
                    sheet.getRow(targetRowIndex).getCell(col_expected_result).setCellValue("OK");
                    if (copy_i == 2) {
                        sheet.getRow(targetRowIndex).getCell(col_value3).setCellValue("正規表現");
                    }
                }
            }

            // コピーした各行にテストデータを記載
            if (isRequiredInput) {
                // 必須
                if (Models.input_type.toUpperCase().equals("TEXTINPUT")
                        || Models.input_type.toUpperCase().equals("TEXTAREA")
                        || Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
                    // テキスト系
                    sheet.getRow(targetRowIndex + 0).getCell(col_keyword).setCellValue("[web]テキスト入力(id)");
                    sheet.getRow(targetRowIndex + 0).getCell(col_value2).setCellValue("");
                    sheet.getRow(targetRowIndex + 1).getCell(col_keyword).setCellValue("[web]value値の確認(id)");
                    sheet.getRow(targetRowIndex + 1).getCell(col_value2).setCellValue("省略出来ません");
                } else if (Models.input_type.toUpperCase().equals("CHECKBOX")) {
                    // チェックボックス
                    // (チェックボックスには、省略という概念が無いので何もしない)
                } else if (Models.input_type.toUpperCase().equals("SELECTINPUT")) {
                    // セレクト
                    sheet.getRow(targetRowIndex + 0).getCell(col_keyword).setCellValue("[web]セレクトボックス選択(id)");
                    String str = "【0】";
                    sheet.getRow(targetRowIndex + 0).getCell(col_value2).setCellValue(str);

                    sheet.getRow(targetRowIndex + 1).getCell(col_keyword).setCellValue("[web]value値の確認(id)");
                    sheet.getRow(targetRowIndex + 1).getCell(col_value2).setCellValue("必ず選択して下さい");
                }
            } else if (isMinOkInput) {
                // 最小OK
                if (Models.input_type.toUpperCase().equals("TEXTINPUT")
                        || Models.input_type.toUpperCase().equals("TEXTAREA")
                        || Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
                    // テキスト系
                    sheet.getRow(targetRowIndex + 0).getCell(col_keyword).setCellValue("[web]テキスト入力(id)");
                    String str = getNormalValue(Models, LengthPattern.MIN, 1);
                    sheet.getRow(targetRowIndex + 0).getCell(col_value2).setCellValue(str);
                    sheet.getRow(targetRowIndex + 1).getCell(col_keyword).setCellValue("[web]value値の確認(id)");
                    if (Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
                        sheet.getRow(targetRowIndex + 1).getCell(col_value2).setCellValue("以上の値にして下さい");
                    } else {
                        sheet.getRow(targetRowIndex + 1).getCell(col_value2).setCellValue("文字以上で入力して下さい");
                    }
                    sheet.getRow(targetRowIndex + 2).getCell(col_keyword).setCellValue("ログ確認(全行)");
                } else if (Models.input_type.toUpperCase().equals("CHECKBOX")) {
                    // チェックボックス
                    // (チェックボックスには、最大値という概念が無いので何もしない)
                } else if (Models.input_type.toUpperCase().equals("SELECTINPUT")) {
                    // セレクト
                    // (セレクトボックスには、最大値という概念が無いので何もしない)
                }
            } else if (isMinNgInput) {
                // 最小NG"
                if (Models.input_type.toUpperCase().equals("TEXTINPUT")
                        || Models.input_type.toUpperCase().equals("TEXTAREA")
                        || Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
                    // テキスト系
                    sheet.getRow(targetRowIndex + 0).getCell(col_keyword).setCellValue("[web]テキスト入力(id)");
                    String strNormalMin = getNormalValue(Models, LengthPattern.MIN, 1);
                    String str = getAbnormalValue(Models, LengthPattern.MIN, 1);

                    sheet.getRow(targetRowIndex + 0).getCell(col_value2).setCellValue(str);
                    sheet.getRow(targetRowIndex + 1).getCell(col_keyword).setCellValue("[web]value値の確認(id)");
                    if (Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
                        sheet.getRow(targetRowIndex + 1).getCell(col_value2).setCellValue(strNormalMin + "以上の値にして下さい");
                    } else {
                        sheet.getRow(targetRowIndex + 1).getCell(col_value2)
                                .setCellValue(Models.MinLength + "文字以上で入力して下さい");
                    }
                } else if (Models.input_type.toUpperCase().equals("CHECKBOX")) {
                    // チェックボックス
                    // (チェックボックスには、最大値という概念が無いので何もしない)
                } else if (Models.input_type.toUpperCase().equals("SELECTINPUT")) {
                    // セレクト
                    // (セレクトボックスには、最大値という概念が無いので何もしない)
                }
            } else if (isMaxOkInput) {
                // 最大OK
                if (Models.input_type.toUpperCase().equals("TEXTINPUT")
                        || Models.input_type.toUpperCase().equals("TEXTAREA")
                        || Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
                    // テキスト系
                    sheet.getRow(targetRowIndex + 0).getCell(col_keyword).setCellValue("[web]テキスト入力(id)");
                    String str = getNormalValue(Models, LengthPattern.MAX, 1);
                    sheet.getRow(targetRowIndex + 0).getCell(col_value2).setCellValue(str);
                    sheet.getRow(targetRowIndex + 1).getCell(col_keyword).setCellValue("[web]value値の確認(id)");
                    if (Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
                        sheet.getRow(targetRowIndex + 1).getCell(col_value2).setCellValue("以下の値にして下さい");
                    } else {
                        sheet.getRow(targetRowIndex + 1).getCell(col_value2).setCellValue("文字以内で入力して下さい");
                    }
                    sheet.getRow(targetRowIndex + 2).getCell(col_keyword).setCellValue("ログ確認(全行)");
                } else if (Models.input_type.toUpperCase().equals("CHECKBOX")) {
                    // チェックボックス
                    // (チェックボックスには、最大値という概念が無いので何もしない)
                } else if (Models.input_type.toUpperCase().equals("SELECTINPUT")) {
                    // セレクト
                    // (セレクトボックスには、最大値という概念が無いので何もしない)
                }
            } else if (isMaxNgInput) {
                // 最大NG
                if (Models.input_type.toUpperCase().equals("TEXTINPUT")
                        || Models.input_type.toUpperCase().equals("TEXTAREA")
                        || Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
                    // テキスト系
                    sheet.getRow(targetRowIndex + 0).getCell(col_keyword).setCellValue("[web]テキスト入力(id)");
                    String strNormalMax = getNormalValue(Models, LengthPattern.MAX, 1);
                    String str = getAbnormalValue(Models, LengthPattern.MAX, 1);

                    sheet.getRow(targetRowIndex + 0).getCell(col_value2).setCellValue(str);
                    sheet.getRow(targetRowIndex + 1).getCell(col_keyword).setCellValue("[web]value値の確認(id)");

                    if (Models.input_type.toUpperCase().equals("NUMBERINPUT")) {
                        sheet.getRow(targetRowIndex + 1).getCell(col_value2).setCellValue(strNormalMax + "以下の値にして下さい");
                    } else {
                        sheet.getRow(targetRowIndex + 1).getCell(col_value2)
                                .setCellValue(Models.MaxLength + "文字以内で入力して下さい");
                    }
                } else if (Models.input_type.toUpperCase().equals("CHECKBOX")) {
                    // チェックボックス
                    // (チェックボックスには、最大値という概念が無いので何もしない)
                } else if (Models.input_type.toUpperCase().equals("SELECTINPUT")) {
                    // セレクト
                    // (セレクトボックスには、最大値という概念が無いので何もしない)
                }
            } else if (isValidationOkInput || isValidationNgInput) {
                // バリデーションOK,バリデーションNG
                // テキスト系
                sheet.getRow(targetRowIndex + 0).getCell(col_keyword).setCellValue("[web]テキスト入力(id)");
                String str = "";
                if (isValidationOkInput) {
                    str = getNormalValue(Models, LengthPattern.MAX, 1);
                } else {
                    str = getAbnormalValue(Models, LengthPattern.NORMAL, 1);
                }
                // 1文字加える
                sheet.getRow(targetRowIndex + 0).getCell(col_value2).setCellValue(str);
                sheet.getRow(targetRowIndex + 1).getCell(col_keyword).setCellValue("[web]value値の確認(id)");
                if (Models.validation_check.equals("全角限定") || Models.validation_check.equals("半角限定")
                        || Models.validation_check.equals("半角英字限定") || Models.validation_check.equals("半角数字限定")
                        || Models.validation_check.equals("半角記号限定") || Models.validation_check.equals("半角カナ限定")
                        || Models.validation_check.equals("全角カナ限定")) {
                    sheet.getRow(targetRowIndex + 1).getCell(col_value2).setCellValue(".*で入力して下さい");
                } else if (Models.validation_check.equals("郵便番号") || Models.validation_check.equals("電話番号")
                        || Models.validation_check.equals("メールアドレス") || Models.validation_check.equals("URL")
                        || Models.validation_check.equals("日付") || Models.validation_check.equals("日時")
                        || Models.validation_check.equals("数値限定") || Models.validation_check.equals("小数点")
                        || Models.validation_check.contains("【正規表現】")) {
                    sheet.getRow(targetRowIndex + 1).getCell(col_value2).setCellValue(".*入力形式が不正です");
                }
                if (isValidationOkInput) {
                    sheet.getRow(targetRowIndex + 2).getCell(col_keyword).setCellValue("ログ確認(全行)");
                }
            }
        }
        TimeMeasurement.logTimeEnd("makeTestRow2:行追加", isDebug);

        if (!isExist && removeRowsFirst != 0) {
            // 生成対象が無い場合は雛形行を削除する
            PoiUtil.removeRow(sheet, removeRowsFirst);
        }

    }

    public void clearControlCharacters(Sheet sheet, int row, boolean isTargetStrLeave) {
        // 指定がなければ制御文字を消す
        if (!isTargetStrLeave) {
            Row tmpRow = sheet.getRow(row);
            if (tmpRow != null && tmpRow.getCell(0) != null) {
                tmpRow.getCell(0).setCellValue("");
            }
        }
    }

    // ランダムパスワード生成
    public static String generateRandomPassword(int length) {
        final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+<>?";
        final SecureRandom RANDOM = new SecureRandom();

        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            password.append(CHARACTERS.charAt(index));
        }
        return password.toString();
    }

    /**
     * テストデータ生成 テーブル名とカラム定義のリストに基づいて、サンプルデータを含むINSERT文を生成します。
     *
     * @param tableName テーブルの名前
     * @param columnDefinitions カラム定義のリスト（各カラムの名前とデータ型を含む）
     * @return サンプルデータを含むINSERT文
     */
    private static String generateInsertStatement(String tableName, List<ColumnDefinition> columnDefinitions,
            String pkColumnName, String pkValue) {
        // INSERT文の初期部分を構築
        StringBuilder insertSql = new StringBuilder("insert into " + tableName + " (");

        // カラム名を列挙してINSERT文に追加
        for (int i = 0; i < columnDefinitions.size(); i++) {
            insertSql.append(columnDefinitions.get(i).getColumnName());
            if (i < columnDefinitions.size() - 1) {
                insertSql.append(", ");
            }
        }

        insertSql.append(") VALUES (");

        // Fakerインスタンスを作成してサンプルデータを生成
        Faker faker = new Faker(new Locale("ja"));
        for (int i = 0; i < columnDefinitions.size(); i++) {
            ColumnDefinition columnDefinition = columnDefinitions.get(i);
            ColDataType columnDataType = columnDefinition.getColDataType();
            String columnType = columnDataType.getDataType().toLowerCase();
            List<String> arguments = columnDataType.getArgumentsStringList();

            if (columnDefinition.getColumnName().toLowerCase().equals(pkColumnName.toLowerCase())) {
                // PK
                insertSql.append(pkValue);
                // システムカラム
            } else if (columnDefinition.getColumnName().toLowerCase().equals("modify_count")) {
                insertSql.append(1);
            } else if (columnDefinition.getColumnName().toLowerCase().equals("delflg")) {
                insertSql.append(false);
            } else if (columnDefinition.getColumnName().toLowerCase().equals("update_date")) {
                insertSql.append("'").append(LocalDateTime.now().toString()).append("'");
            } else if (columnDefinition.getColumnName().toLowerCase().equals("update_u_id")) {
                insertSql.append(1);
            } else {

                // データ型の簡略化
                if (columnType.contains("varchar")) {
                    columnType = "varchar";
                } else if (columnType.contains("character")) {
                    columnType = "character";
                } else if (columnType.contains("char")) {
                    columnType = "character";
                } else if (columnType.contains("numeric")) {
                    columnType = "numeric";
                }

                // データ型に応じてサンプルデータを生成
                switch (columnType) {
                case "boolean":
                    insertSql.append(faker.bool().bool()); // ランダムな真偽値を生成
                    break;
                case "smallint":
                    insertSql.append(faker.number().numberBetween(1, 32767)); // ランダムなShort値を生成
                    break;
                case "integer":
                    insertSql.append(faker.number().numberBetween(1, 2147483647)); // ランダムなInteger値を生成
                    break;
                case "bigint":
                    insertSql.append(faker.number().randomNumber()); // ランダムなLong値を生成
                    break;
                case "real":
                    insertSql.append(faker.number().randomDouble(2, 1, 100)); // ランダムなFloat値を生成
                    break;
                case "double precision":
                    insertSql.append(faker.number().randomDouble(4, 1, 10000)); // ランダムなDouble値を生成
                    break;
                case "numeric":
                    insertSql.append(faker.number().randomDouble(2, 1, 100)); // ランダムなBigDecimal値を生成
                    break;
                case "text":
                case "varchar":
                case "character":
                    int length = arguments != null && !arguments.isEmpty() ? Integer.parseInt(arguments.get(0)) : 10;
                    insertSql.append("'").append(faker.lorem().characters(length)).append("'"); // サンプル文字列を生成
                    break;
                case "bytea":
                    insertSql.append("NULL"); // byte配列の場合はNULL
                    break;
                case "timestamp":
                    insertSql.append("'").append(faker.date().birthday().toInstant().toString()).append("'"); // サンプルTimestampを生成
                    break;
                case "date":
                    insertSql.append("'").append(faker.date().birthday().toInstant().toString().substring(0, 10))
                            .append("'"); // サンプルDateを生成
                    break;
                case "time":
                    insertSql.append("'").append(faker.date().birthday().toInstant().toString().substring(11, 19))
                            .append("'"); // サンプルTimeを生成
                    break;
                case "smallserial":
                    insertSql.append(faker.number().numberBetween(1, 32767)); // ランダムなShort値を生成
                    break;
                case "serial":
                    insertSql.append(faker.number().numberBetween(1, 2147483647)); // ランダムなInteger値を生成
                    break;
                case "bigserial":
                    insertSql.append(faker.number().randomNumber()); // ランダムなLong値を生成
                    break;
                // 他のデータ型に応じて必要なサンプルデータを追加
                default:
                    insertSql.append("NULL"); // 未対応のデータ型の場合はNULL
                }
            }

            if (i < columnDefinitions.size() - 1) {
                insertSql.append(", ");
            }
        }

        insertSql.append(");");

        // 完成したINSERT文を返す
        return insertSql.toString();
    }

}
