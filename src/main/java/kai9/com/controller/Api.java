package kai9.com.controller;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import kai9.libs.JsonResponse;
import com.google.gson.Gson;

@RestController
//@CrossOrigin(origins = "https://kai9.com:3000")無くても動く(WebConfiguration側で設定済だからだと思う)
@CrossOrigin
public class Api {

    @Autowired
    @Qualifier("commonjdbc")
    JdbcTemplate jdbcTemplate;

    @GetMapping("/api/test")
    public String test() {

        return "TEST OK";

    }

    // 有効な認証がある場合return_codeを0で返す(ここに到達していれば認可OKというロジック)
    @PostMapping("/api/check-auth")
    public void check_auth(HttpServletResponse res) throws IOException, JSONException {

        // 現在のログインしているユーザ名をSPRING SECURITYで取得
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = "";
        if (authentication != null) {
            // AuthenticationオブジェクトからUserDetailsオブジェクトを取得
            Object principal = authentication.getPrincipal();
            name = principal.toString();
        }

        // ログインIDからユーザ情報を取得

        String sql = "SELECT * FROM m_user_a WHERE login_id = ?";
        try {
            Map<String, Object> map = jdbcTemplate.queryForMap(sql, name);// ヒットしない場合例外が発生しcatch区へ遷移する

            // 各情報を返す
            // JSON形式でレスポンスを返す
            JsonResponse json = new JsonResponse();
            json.setReturn_code(HttpStatus.OK.value());
            json.setMsg("check-auth-OK:" + name);
            json.Add("user_id", String.valueOf(map.get("user_id")));
            json.Add("login_id", String.valueOf(map.get("login_id")));
            json.Add("modify_count", String.valueOf(map.get("modify_count")));
            json.Add("default_g_id", String.valueOf(map.get("default_g_id")));
            json.Add("authority_lv", String.valueOf(map.get("authority_lv")));
            json.SetJsonResponse(res);
            return;

        } catch (Exception e) {
            // ユーザが存在しなければエラーを返す
            // JSON形式でレスポンスを返す
            JsonResponse authresult = new JsonResponse();
            authresult.setMsg("check-auth-NG:" + name);
            authresult.SetJsonResponse(res);
            return;
        }
    }

    @PostMapping("/api/signout")
    public String signout(HttpServletRequest req, HttpServletResponse response) {

        // cookieの削除をブラウザへ指示
        // https://qiita.com/hitsumabushi845/items/e2f3467c1493b0dae932
        Cookie cookies[] = req.getCookies();
        for (Cookie cookie : cookies) {
            if ("token".equals(cookie.getName())) {
                // ドメインをapplication.ymlからロード
                YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
                factory.setResources(new ClassPathResource("application.yml"));
                Properties properties = factory.getObject();
                String jwt_domain = properties.getProperty("jwt.domain");

                // 有効期限を0にし、値も空で返せば消える
                String cookieStr = String.format("%s=%s; HttpOnly; Secure; SameSite=None; Domain=" + jwt_domain + "; Max-Age=0; Path=/", "token", "");
                response.addHeader("Set-Cookie", cookieStr);
            }
        }
        // JSON形式でレスポンスを返す
        Gson gson = new Gson();
        JsonResponse authresult = new JsonResponse();
        authresult.setMsg("OK");
        String res = gson.toJson(authresult);
        return res;
    }

}