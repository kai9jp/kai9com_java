package kai9.com.model;

//日付型
import java.util.Date;

//テーブルカラム指定
import javax.persistence.Column;
//テーブル構造オブジェクト
import javax.persistence.Entity;
//自動採番キー
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
//主キー
import javax.persistence.Id;
//テーブル名との紐付
import javax.persistence.Table;

//非DB項目
import org.springframework.data.annotation.Transient;

//Gatter、Setterの自動生成
import lombok.Data;

/**
 * @Entity：Entityクラスであることを宣言する
 * @Table：name属性で連携するテーブル名を指定する
 */
@Entity
@Table(name = "m_group1_a")
@Data
public class m_group1 {
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
    @Column(name = "group_id")
    private int group_id;

    @Column(name = "modify_count1")
    private int modify_count1;
    @Column(name = "group_name")
    private String group_name;
    @Column(name = "note")
    private String note;
    @Column(name = "update_u_id")
    private int update_u_id;
    @Column(name = "update_date")
    private Date update_date;
    @Column(name = "delflg")
    private boolean delflg;

    // 非DB項目
    @Transient
    transient int user_count;

    /**
     * @param user_id
     * @param modify_count1
     * @param group_name
     * @param note
     * @param update_u_id
     * @param update_date
     * @param delflg
     */
    public m_group1(int group_id, int modify_count1, String group_name, String note, int update_u_id, Date update_date, boolean delflg) {
        this.group_id = group_id;
        this.modify_count1 = modify_count1;
        this.group_name = group_name;
        this.note = note;
        this.update_u_id = update_u_id;
        this.update_date = update_date;
        this.delflg = delflg;
        this.user_count = 0;
    }

    public m_group1() {
    }// コンソトラクタスタブ

}
