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
@IdClass(m_group2_a_key.class)
class m_group2_a_key implements Serializable {
    /**
     * 複合キー用のクラス
     */
    @Embedded
    private int group_id;
    private int user_id;
}

/**
 * @Entity：Entityクラスであることを宣言する
 * @Table：name属性で連携するテーブル名を指定する
 */
@Entity
@Table(name = "m_group2_a")
@Data
@IdClass(m_group2_a_key.class)
public class m_group2 {
    /**
     * 
     */

    /**
     * @Id：主キーに指定する。※複合キーの場合は@EmbeddedIdを使用
     * @GeneratedValue：主キーの指定をJPAに委ねる
     * @Column：name属性でマッピングするカラム名を指定する
     */
    @Id
    @Column(name = "group_id")
    private int group_id;
    @Column(name = "modify_count1")
    private int modify_count1;
    @Id
    @Column(name = "user_id")
    private int user_id;
    @Column(name = "modify_count2")
    private int modify_count2;
    @Column(name = "update_u_id")
    private int update_u_id;
    @Column(name = "update_date")
    private Date update_date;
    @Column(name = "delflg")
    private boolean delflg;

    /**
     * @param user_id
     * @param modify_count1
     * @param group_name
     * @param note
     * @param update_u_id
     * @param update_date
     * @param delflg
     */
    public m_group2(int group_id, int modify_count1, int user_id, int modify_count2, int update_u_id, Date update_date, boolean delflg) {
        this.group_id = group_id;
        this.modify_count1 = modify_count1;
        this.user_id = user_id;
        this.modify_count2 = modify_count2;
        this.update_u_id = update_u_id;
        this.update_date = update_date;
        this.delflg = delflg;
    }

    public m_group2() {
        // TODO 自動生成されたコンストラクター・スタブ
    }

}
