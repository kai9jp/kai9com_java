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
@Table(name = "m_server_a")
@Data
public class m_server {
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
    @Column(name = "server_id")
    private int server_id;

    @Column(name = "modify_count")
    private int modify_count;
    @Column(name = "host_name")
    private String host_name;
    @Column(name = "ip")
    private String ip;
    @Column(name = "note")
    private String note;
    @Column(name = "update_u_id")
    private int update_u_id;
    @Column(name = "update_date")
    private Date update_date;
    @Column(name = "delflg")
    private boolean delflg;

    /**
     * 
     */
    public m_server(int server_id, int modify_count, String host_name, String ip,
            String note, int update_u_id, Date update_date, boolean delflg) {
        this.server_id = server_id;
        this.modify_count = modify_count;
        this.host_name = host_name;
        this.ip = ip;
        this.note = note;
        this.update_u_id = update_u_id;
        this.update_date = update_date;
        this.delflg = delflg;
    }

    public m_server() {
        // TODO 自動生成されたコンストラクター・スタブ
    }

}
