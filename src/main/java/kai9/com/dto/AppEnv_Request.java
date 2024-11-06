package kai9.com.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 環境マスタ:リクエストデータ
 */
@SuppressWarnings("serial")
@Data
public class AppEnv_Request implements Serializable {

    /**
     * 更新回数
     */
    private Integer modify_count;

    /**
     * 一時フォルダ
     */
    private String dir_tmp;

    /**
     * 一時フォルダ保持期間
     */
    private Integer del_days_tmp;

    /**
     * SVN_React_フォルダ
     */
    private String svn_react_dir;

    /**
     * SVN_Spring_フォルダ
     */
    private String svn_spring_dir;

    /**
     * SVN_Scenario_フォルダ
     */
    private String svn_scenario_dir;

    /**
     * SVN_Testdata_フォルダ
     */
    private String svn_testdata_dir;

    /**
     * SVN_React_URL
     */
    private String svn_react_url;

    /**
     * SVN_Scenario_URL
     */
    private String svn_scenario_url;

    /**
     * SVN_Spring_URL
     */
    private String svn_spring_url;

    /**
     * SVN_Testdata_URL
     */
    private String svn_testdata_url;

    /**
     * SVN_ID
     */
    private String svn_id;

    /**
     * SVNパスワード
     */
    private String svn_pw;

    /**
     * SVNパスワードsalt
     */
    private String svn_pw_salt;

    /**
     * 更新者
     */
    private Integer update_u_id;

    /**
     * 更新日時
     */
    private Date update_date;

}
