package kai9.com.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import lombok.Data;

@Embeddable
@Data
@SuppressWarnings("serial")
class m_jobserver_a_key implements Serializable {
    /**
     * 複合キー用のクラス
     */
    @Embedded
    private int jobinfo_id;
    private int server_id;
}

/**
 * @Entity：Entityクラスであることを宣言する
 * @Table：name属性で連携するテーブル名を指定する
 */
@Entity
@Table(name = "m_jobserver_a")
@Data
@IdClass(m_jobserver_a_key.class)
public class m_jobserver {
    /**
     * @Id：主キーに指定する。※複合キーの場合は@EmbeddedIdを使用
     * @GeneratedValue：主キーの指定をJPAに委ねる
     * @Column：name属性でマッピングするカラム名を指定する
     */
    @Id
    @Column(name = "jobinfo_id")
    private int jobinfo_id;
    @Id
    @Column(name = "server_id")
    private int server_id;
    @Column(name = "modify_count")
    private int modify_count;
    @Column(name = "update_u_id")
    private int update_u_id;
    @Column(name = "update_date")
    private Date update_date;
    @Column(name = "delflg")
    private boolean delflg;

    /**
     */
    public m_jobserver(int jobinfo_id, int server_id, int modify_count, int update_u_id, Date update_date, boolean delflg) {
        this.jobinfo_id = jobinfo_id;
        this.server_id = server_id;
        this.modify_count = modify_count;
        this.update_u_id = update_u_id;
        this.update_date = update_date;
        this.delflg = delflg;
    }

    public m_jobserver() {
        // TODO 自動生成されたコンストラクター・スタブ
    }

}
