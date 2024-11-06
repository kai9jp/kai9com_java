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
@Table(name = "m_jobinfo_a")
@Data
public class m_jobinfo {
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
    @Column(name = "jobinfo_id")
    private int jobinfo_id;

    @Column(name = "modify_count")
    private int modify_count;
    @Column(name = "job_name")
    private String job_name;
    @Column(name = "unit_full_name")
    private String unit_full_name;
    @Column(name = "last_exec_datetime")
    private Date last_exec_datetime;
    @Column(name = "next_exec_datetime")
    private Date next_exec_datetime;
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
    public m_jobinfo(int jobinfo_id, int modify_count, String job_name, String unit_full_name, Date last_exec_datetime, Date next_exec_datetime,
            String note, int update_u_id, Date update_date, boolean delflg) {
        this.jobinfo_id = jobinfo_id;
        this.modify_count = modify_count;
        this.job_name = job_name;
        this.unit_full_name = unit_full_name;
        this.last_exec_datetime = last_exec_datetime;
        this.next_exec_datetime = next_exec_datetime;
        this.note = note;
        this.update_u_id = update_u_id;
        this.update_date = update_date;
        this.delflg = delflg;
    }

    public m_jobinfo() {
        // TODO 自動生成されたコンストラクター・スタブ
    }

}
