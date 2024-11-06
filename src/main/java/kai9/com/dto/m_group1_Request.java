package kai9.com.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * グループー情報 リクエストデータ
 */
@Data
@SuppressWarnings("serial")
public class m_group1_Request implements Serializable {

    /**
     * グループID
     */
    private int group_id;

    /**
     * 更新回数
     */
    private int modify_count1;

    /**
     * グループ名
     */
    private String group_name;

    /**
     * 備考
     */
    private String note;

    private int update_u_id;
    private Date update_date;
    private boolean delflg;

}