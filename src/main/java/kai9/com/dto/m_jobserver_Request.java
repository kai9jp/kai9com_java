package kai9.com.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * ジョブ対象サーバ リクエストデータ
 */
@Data
@SuppressWarnings("serial")
public class m_jobserver_Request implements Serializable {

    /**
     * ジョブ情報ID
     */
    private Integer jobinfo_id;

    /**
     * サーバID
     */
    private Integer server_id;

    /**
     * 更新回数
     */
    private Integer modify_count;

    private Integer update_u_id;
    private Date update_date;
    private boolean delflg;

}