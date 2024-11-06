package kai9.com.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

/**
 * @Entity：Entityクラスであることを宣言する
 * @Table：name属性で連携するテーブル名を指定する
 */
@Entity
@Table(name = "m_user_a")
@Data
public class m_user {
    /**
     * 
     */

    /**
     * @Id：主キーに指定する。※複合キーの場合は@EmbeddedIdを使用
     * @GeneratedValue：主キーの指定をJPAに委ねる
     * @Column：name属性でマッピングするカラム名を指定する
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int user_id;

    @Column(name = "modify_count")
    private int modify_count;
    @Column(name = "login_id")
    private String login_id;
    @Column(name = "sei")
    private String sei;
    @Column(name = "mei")
    private String mei;
    @Column(name = "sei_kana")
    private String sei_kana;
    @Column(name = "mei_kana")
    private String mei_kana;
    @Column(name = "password")
    private String password;
    @Column(name = "need_password_change")
    private boolean need_password_change;
    @Column(name = "mail")
    private String mail;
    @Column(name = "ip")
    private String ip;
    @Column(name = "default_g_id")
    private int default_g_id;
    @Column(name = "authority_lv")
    private int authority_lv;
    @Column(name = "note")
    private String note;
    @Column(name = "update_u_id")
    private int update_u_id;
    @Column(name = "update_date")
    // @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")//https://tech.excite.co.jp/entry/2021/04/23/215926 Java→ReactはOKだが、逆が400エラーになり使えず。react側のmomentで回避
    private Date update_date;
    @Column(name = "delflg")
    private boolean delflg;

    /**
     * 表示専用フィールド
     **/
    public String authority_lv_view() {
        switch (authority_lv) {
        case 1:
            return "1:一般";
        case 2:
            return "2:参照専用";
        case 3:
            return "3:管理者";
        default:
            return "?";
        }
    }

    /**
     * @param user_id
     * @param modify_count
     * @param login_id
     * @param sei
     * @param mei
     * @param sei_kana
     * @param mei_kana
     * @param password
     * @param need_password_change
     * @param mail
     * @param ip
     * @param default_g_id
     * @param authority_lv
     * @param note
     * @param update_u_id
     * @param update_date
     * @param delflg
     */
    public m_user(int user_id, int modify_count, String login_id, String sei, String mei, String sei_kana,
            String mei_kana, String password, boolean need_password_change, String mail, String ip, int default_g_id,
            int authority_lv, String note, int update_u_id, Date update_date, boolean delflg) {
        this.user_id = user_id;
        this.modify_count = modify_count;
        this.login_id = login_id;
        this.sei = sei;
        this.mei = mei;
        this.sei_kana = sei_kana;
        this.mei_kana = mei_kana;
        this.password = password;
        this.need_password_change = true;
        this.mail = mail;
        this.ip = ip;
        this.default_g_id = default_g_id;
        this.authority_lv = authority_lv;
        this.note = note;
        this.update_u_id = update_u_id;
        this.update_date = update_date;
        this.delflg = delflg;
    }

    public m_user() {
        // TODO 自動生成されたコンストラクター・スタブ
    }

}
