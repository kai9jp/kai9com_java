package kai9.com.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

/**
 * 環境マスタ :モデル
 */
@Entity
@Data
@Table(name = "app_env_a")
public class AppEnv {

    /**
     * 更新回数
     */
    @Id
    @Column(name = "modify_count")
    private Integer modify_count;

    /**
     * 一時フォルダ
     */
    @Column(name = "dir_tmp")
    private String dir_tmp;

    /**
     * 一時フォルダ保持期間
     */
    @Column(name = "del_days_tmp")
    private Integer del_days_tmp;

    /**
     * SVN_React_フォルダ
     */
    @Column(name = "svn_react_dir")
    private String svn_react_dir;

    /**
     * SVN_Scenario_フォルダ
     */
    @Column(name = "svn_scenario_dir")
    private String svn_scenario_dir;

    /**
     * SVN_Spring_フォルダ
     */
    @Column(name = "svn_spring_dir")
    private String svn_spring_dir;

    /**
     * SVN_Testdata_フォルダ
     */
    @Column(name = "svn_testdata_dir")
    private String svn_testdata_dir;

    /**
     * SVN_React_URL
     */
    @Column(name = "svn_react_url")
    private String svn_react_url;

    /**
     * SVN_Spring_URL
     */
    @Column(name = "svn_spring_url")
    private String svn_spring_url;

    /**
     * SVN_Scenario_URL
     */
    @Column(name = "svn_scenario_url")
    private String svn_scenario_url;

    /**
     * SVN_Testdata_URL
     */
    @Column(name = "svn_testdata_url")
    private String svn_testdata_url;

    /**
     * SVN_ID
     */
    @Column(name = "svn_id")
    private String svn_id;

    /**
     * SVNパスワード
     */
    @Column(name = "svn_pw")
    private String svn_pw;

    /**
     * SVNパスワードsalt
     */
    @Column(name = "svn_pw_salt")
    private String svn_pw_salt;

    /**
     * 更新者
     */
    @Column(name = "update_u_id")
    private Integer update_u_id;

    /**
     * 更新日時
     */
    @Column(name = "update_date")
    private Date update_date;

    public AppEnv() {
    }// コンストラクタ スタブ

}
