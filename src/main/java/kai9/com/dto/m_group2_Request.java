package kai9.com.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * グループー情報 リクエストデータ
 */
@Data
@SuppressWarnings("serial")
public class m_group2_Request implements Serializable {

    /**
     * グループID
     */
    private Integer group_id;

    /**
     * 更新回数
     */
    private Integer modify_count1;

    /**
     * ユーザID
     */
    private Integer user_id;

    /**
     * 更新回数2
     */
    private Integer modify_count2;

    private Integer update_u_id;
    private Date update_date;
    private boolean delflg;

}