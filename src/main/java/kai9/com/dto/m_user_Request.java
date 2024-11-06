package kai9.com.dto;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Data;

/**
 * ユーザー情報 リクエストデータ
 */
@Data
@SuppressWarnings("serial")
public class m_user_Request implements Serializable {

    /**
     * ユーザID
     */
    private int user_id;

    /**
     * 更新回数
     */
    private int modify_count;

    /**
     * ログインID
     */
    @Size(max = 20, message = "ログインIDは20桁以内で入力してください")
    // 半角英大文字と半角英小文字と数字を含む(肯定先読みを使う)
    @Pattern(regexp = "^([a-z]*|[A-Z]*|[0-9]*)*$", message = "ログインIDは半角英数で入力して下さい")
    private String login_id;

    /**
     * 姓
     */
    @NotBlank(message = "姓を入力してください")
    @Size(max = 20, message = "姓は20桁以内で入力してください")
    private String sei;

    /**
     * 名
     */
    @NotBlank(message = "名を入力してください")
    @Size(max = 20, message = "名は20桁以内で入力してください")
    private String mei;

    /**
     * セイ
     */
    @NotBlank(message = "セイを入力してください")
    @Size(max = 40, message = "セイは40桁以内で入力してください")
    // 全角カナ、半角カナ
    @Pattern(regexp = "^([ァ-ヶー　]|[ｦ-ﾟ ])*$|", message = "セイは全角・半角カナで入力してください")
    private String sei_kana;

    /**
     * メイ
     */
    @NotBlank(message = "メイを入力してください")
    @Size(max = 40, message = "メイは40桁以内で入力してください")
    @Pattern(regexp = "^([ァ-ヶー　]|[ｦ-ﾟ ])*$|", message = "メイは全角・半角カナで入力してください")
    private String mei_kana;

    /**
     * パスワード
     */
    @NotBlank(message = "パスワードを入力してください")
    private String password;

    /**
     * パスワード変更要求
     */
    private boolean need_password_change;

    public boolean getNeed_password_change() {
        // TODO 自動生成されたメソッド・スタブ
        return this.need_password_change;
    }

    /**
     * メールアドレス
     */
    @NotBlank(message = "メールアドレスを入力してください")
    @Size(max = 200, message = "メールアドレスは200桁以内で入力してください")
    @Email
    private String mail;

    /**
     * 利用端末IPアドレス
     * https://qiita.com/BRSF/items/2a83af4e605019e8c2fe 正規表現で値なしor条件付き入力の制御
     */
    @Size(max = 15, message = "利用端末IPアドレスは15桁以内で入力してください")
    @Pattern(regexp = "^((([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]).){3}([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))?$", message = "IPアドレスとして正しい形式にしてください")
    private String ip;

    /**
     * デフォルトグループ
     */
    private int default_g_id;

    /**
     * 権限レベル
     */
    private int authority_lv;

    /**
     * 備考
     */
    @Size(max = 200, message = "備考は200桁以内で入力してください")
    private String note;

    private int update_u_id;
    private Date update_date;
    private boolean delflg;

}