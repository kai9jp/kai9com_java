package kai9.com.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kai9.com.srcmake.JavaMaker;
import kai9.libs.Kai9Utils;
import kai9.com.srcmake.ReactMaker;
import kai9.com.srcmake.ScenarioMaker;
import kai9.libs.SvnUtils;
import kai9.libs.ZipUtility;
import kai9.libs.JsonResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kai9.com.dto.AppEnv_Request;
import kai9.com.model.AppEnv;
import kai9.com.service.ProgressStatus_Service;
import kai9.com.service.AppEnv_Service;

/**
 * 処理設定_親 :コントローラ
 */
@RestController
public class make_source_code_Controller {

    @Autowired
    private AppEnv_Service AppEnv_Service;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ProgressStatus_Service progressStatus_Service;

    // 件数格納用の変数(スレッドセーフ)
    private ThreadLocal<Integer> progressStatusId = ThreadLocal.withInitial(() -> 0);

    /**
     * CUD操作
     */
//  public void create(@RequestBody AppEnv_Request m_env,HttpServletResponse res,HttpServletRequest request) throws CloneNotSupportedException, IOException, JSONException {

    @PostMapping(value = { "/api/make_source_code" }, produces = "application/json;charset=utf-8")
    public ResponseEntity<Resource> make_source_code(
            @RequestPart(value = "tdd_excel", required = false)
            MultipartFile file_excel,
            @RequestParam
            Map<String, String> requestData,
            HttpServletResponse res, HttpServletRequest request) throws CloneNotSupportedException, IOException, JSONException {
        Kai9Utils.makeLog("info", "001,", this.getClass());

        String sheetNamesJson = requestData.get("sheet_names");
        String isTargetStrLeaveStr = requestData.get("isTargetStrLeave");
        String isNonTestMakeStr = requestData.get("isNonTestMake");
        String projectName = requestData.get("project_name");
        String packageName1 = requestData.get("packageName1");
        String packageName2 = requestData.get("packageName2");
        String progress_status_id = requestData.get("progress_status_id");

        InputStream excelInputStream = null;
        try {

            // 進捗管理
            this.progressStatusId.set(Integer.valueOf(progress_status_id));
            progressStatus_Service.init(progressStatusId.get());
            progressStatus_Service.updateProgress2(progressStatusId.get(), "無し", -1, "無し");
            progressStatus_Service.setMaxValue1(progressStatusId.get(), 6);
            progressStatus_Service.setCurrentValue1(progressStatusId.get(), 1);

            if (file_excel == null) {
                // JSON形式でレスポンスを返す
                JsonResponse json = new JsonResponse();
                json.setReturn_code(HttpStatus.BAD_REQUEST.value());
                json.setMsg("【テーブル定義書】　のエクセルがバックエンド側に届きませんでした。");
                json.SetJsonResponse(res);
                return null;
            }
            ;
            if (sheetNamesJson == null) {
                // JSON形式でレスポンスを返す
                JsonResponse json = new JsonResponse();
                json.setReturn_code(HttpStatus.BAD_REQUEST.value());
                json.setMsg("【テーブル定義書】の【対象シート情報】がバックエンド側に届きませんでした。");
                json.SetJsonResponse(res);
                return null;
            }
            ;

            boolean isTargetStrLeave = Boolean.parseBoolean(isTargetStrLeaveStr);
            boolean isNonTestMake = Boolean.parseBoolean(isNonTestMakeStr);

            // 環境マスタ読込
            AppEnv m_env = AppEnv_Service.findById();

            // 複合化
            String salt = m_env.getSvn_pw_salt();
            String secretKey = "kai9SecretKey"; // 実際のアプリケーションでは安全な文字列で運用する事(AppEnv_Serviceと対)
            TextEncryptor encryptor = Encryptors.text(secretKey, salt);
            String Svn_pw = encryptor.decrypt(m_env.getSvn_pw());
            Kai9Utils.makeLog("info", "002,", this.getClass());

            // 進捗
            progressStatus_Service.setCurrentValue1(progressStatusId.get(), 2);

            // SVNでkai9templateのソースコード最新版を取得
            deleteDirectory(m_env.getSvn_react_dir(), true);
            Kai9Utils.makeLog("info", "002-A-1,", this.getClass());
            SvnUtils.updateSvnFolder(m_env.getSvn_react_url(), m_env.getSvn_react_dir(), m_env.getSvn_id(), Svn_pw, false, true);
            Kai9Utils.makeLog("info", "002-A-2,", this.getClass());
            // 進捗
            progressStatus_Service.setCurrentValue1(progressStatusId.get(), 3);
            deleteDirectory(m_env.getSvn_spring_dir(), true);
            SvnUtils.updateSvnFolder(m_env.getSvn_spring_url(), m_env.getSvn_spring_dir(), m_env.getSvn_id(), Svn_pw, false, true);
            if (!isNonTestMake) {
                // SVNでkai9template(シングルテーブル)の処理シナリオ最新版を取得
                deleteDirectory(m_env.getSvn_scenario_dir(), true);
                SvnUtils.updateSvnFolder(m_env.getSvn_scenario_url(), m_env.getSvn_scenario_dir(), m_env.getSvn_id(), Svn_pw, true, false);
                // 進捗
                progressStatus_Service.setCurrentValue1(progressStatusId.get(), 4);
                // SVNでkai9template(シングルテーブル)のテストデータを取得
                deleteDirectory(m_env.getSvn_testdata_dir(), true);
                SvnUtils.updateSvnFolder(m_env.getSvn_testdata_url(), m_env.getSvn_testdata_dir(), m_env.getSvn_id(), Svn_pw, false, true);
                Kai9Utils.makeLog("info", "002-1,", this.getClass());
            }

            // 進捗
            progressStatus_Service.setCurrentValue1(progressStatusId.get(), 5);

            // シート名の読込
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode sheetNamesNode = objectMapper.readTree(sheetNamesJson);
            Kai9Utils.makeLog("info", "002-2,", this.getClass());

            // エクセルファイルの読込
            excelInputStream = file_excel.getInputStream();
            Workbook excel = WorkbookFactory.create(excelInputStream);
            Kai9Utils.makeLog("info", "002-3,", this.getClass());

            // TEMPフォルダのパスを生成
            String tempFolderPath = m_env.getDir_tmp();
            // 現在の日付時刻を取得
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String folderName = currentDateTime.format(formatter);
            // フォルダを作成
            Path newFolderPath = Paths.get(tempFolderPath, folderName);
            File newFolder = newFolderPath.toFile();
            Kai9Utils.makeLog("info", "002-4,", this.getClass());
            if (!newFolder.mkdirs()) {
                Kai9Utils.makeLog("info", "002-5," + newFolderPath.toFile(), this.getClass());
                // JSON形式でレスポンスを返す
                JsonResponse json = new JsonResponse();
                json.setReturn_code(HttpStatus.CONFLICT.value());
                json.setMsg("フォルダ作成失敗:" + newFolderPath);
                json.SetJsonResponse(res);
                return null;
            }

            Kai9Utils.makeLog("info", "003,", this.getClass());

            // 進捗用
            progressStatus_Service.setMaxValue2(progressStatusId.get(), sheetNamesNode.size() * 3);
            progressStatus_Service.setCurrentValue2(progressStatusId.get(), 0);
            int progressCounter = 0;

            for (int i1 = 1; i1 < excel.getNumberOfSheets(); i1++) {
                // 対象シートの特定
                Sheet ws = excel.getSheetAt(i1);
                // シート名を取得
                String sheetName = ws.getSheetName();
                // APIで受け取ったシート名と一致すれば処理する
                if (sheetNamesNode.isArray()) {
                    for (JsonNode node : sheetNamesNode) {
                        if (node.asText().equals(sheetName)) {
                            // ---------------------------------------------------
                            // Reactソース生成
                            if (!ReactMaker.Make(ws, newFolderPath.toString(), m_env.getSvn_react_dir(), isTargetStrLeave)) {
                                // Makeメソッド内のエラーは例外をスローするので、ここには届いた場合は、予期せぬエラーだけ
                                // JSON形式でレスポンスを返す
                                JsonResponse json = new JsonResponse();
                                json.setReturn_code(HttpStatus.CONFLICT.value());
                                json.setMsg("Reactソース生成失敗:" + newFolderPath);
                                json.SetJsonResponse(res);
                                return null;
                            }
                            // 進捗用
                            progressCounter++;
                            progressStatus_Service.setCurrentValue2(progressStatusId.get(), progressCounter);

                            // ---------------------------------------------------
                            // Javaソース生成
                            JavaMaker.Make(ws, newFolderPath.toString(), m_env.getSvn_spring_dir(), isTargetStrLeave, packageName1, packageName2);
                            // 進捗用
                            progressCounter++;
                            progressStatus_Service.setCurrentValue2(progressStatusId.get(), progressCounter);

                            if (!isNonTestMake) {
                                // ---------------------------------------------------
                                // 処理シナリオ生成
                                ScenarioMaker ScenarioMaker = context.getBean(ScenarioMaker.class);
                                ScenarioMaker.Make(excel, ws, newFolderPath.toString(), projectName.toLowerCase(), isTargetStrLeave);
                            }
                            // 進捗用
                            progressCounter++;
                            progressStatus_Service.setCurrentValue2(progressStatusId.get(), progressCounter);
                        }
                    }
                }
            }

            Kai9Utils.makeLog("info", "004,", this.getClass());
            ZipUtility.zipFolder(newFolderPath.toString(), newFolderPath.toString() + ".zip");

            File file = new File(newFolderPath.toString() + ".zip");
            byte[] fileContent = Files.readAllBytes(file.toPath());
            ByteArrayResource resource = new ByteArrayResource(fileContent);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + folderName + ".zip");
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            // 進捗管理(100%で表示)
            progressStatus_Service.updateProgress1(progressStatusId.get(), "完了", 100, "完了");
            Thread.sleep(1000);

            Kai9Utils.makeLog("info", "005,", this.getClass());
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            Kai9Utils.makeLog("info", "006,", this.getClass());
            return Kai9Utils.handleExceptionAsResponseEntity(e, res, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            Kai9Utils.makeLog("info", "007,", this.getClass());
            // 解放しないとspringログに一時ファイルを消せないというエラーが出るので対応
            if (excelInputStream != null) {
                excelInputStream.close();
                excelInputStream = null;
            }
        }
    }

    /**
     * 指定されたディレクトリとその内容を削除します。
     *
     * @param directoryPath 削除するディレクトリのパス
     * @param keepTopLevel true の場合、最上位ディレクトリは保持されます。それ以外の場合、削除されます
     * @throws IOException 入出力エラーが発生した場合
     */
    public static void deleteDirectory(String directoryPath, boolean keepTopLevel) throws IOException {
        // 文字列のパスを Path オブジェクトに変換
        Path path = Paths.get(directoryPath);

        // ディレクトリが存在するか確認
        if (Files.exists(path)) {
            // 指定されたパスからファイルツリーを辿る
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

                // ファイルを訪問する度に呼び出されるメソッド
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        // ファイルを削除
                        Files.delete(file);
                    } catch (IOException e) {
                        // ファイル削除に失敗した場合のエラーメッセージを表示
                        System.err.println("Failed to delete file: " + file + " - " + e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }

                // ディレクトリを訪問した後に呼び出されるメソッド
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    // 現在のディレクトリが最上位ディレクトリでない、または最上位ディレクトリを保持しない場合
                    if (!dir.equals(path) || !keepTopLevel) {
                        try {
                            // ディレクトリを削除
                            Files.delete(dir);
                        } catch (IOException e) {
                            // ディレクトリ削除に失敗した場合のエラーメッセージを表示
                            System.err.println("Failed to delete directory: " + dir + " - " + e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * ファイルダウンロード(excel)
     */
//  	@PostMapping(value = "/api/m_keyword1_excel_download", produces = "application/json;charset=utf-8")
//	public ResponseEntity<Resource >  m_keyword1_excel_download(int modify_count) throws CloneNotSupportedException, IOException {
//  		
//		String sql = "select * from m_keyword1_b where modify_count = ?";
//        RowMapper<m_keyword1> rowMapper = new BeanPropertyRowMapper<m_keyword1>(m_keyword1.class);
//        m_keyword1 m_keyword1 = jdbcTemplate.queryForObject(sql,rowMapper,modify_count);
//
//        ByteArrayResource resource = new ByteArrayResource(m_keyword1.getExcel());
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + m_keyword1.getExcel_filename());
//        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
//        headers.add("Pragma", "no-cache");
//        headers.add("Expires", "0");	        
//        
//        return ResponseEntity
//        		.ok()
//        		.headers(headers)
//                .contentLength(m_keyword1.getExcel().length)
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .body(resource);	        
//  	}
//

}
