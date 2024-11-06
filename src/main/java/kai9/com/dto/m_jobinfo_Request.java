package kai9.com.dto;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;

/**
 * ジョブ情報情報 リクエストデータ
 */
@Data
@SuppressWarnings("serial")
public class m_jobinfo_Request implements Serializable {

    /**
     * ジョブ情報ID
     */
    private int jobinfo_id;

    /**
     * 更新回数
     */
    private int modify_count;

    @NotBlank(message = "ジョブ名を入力してください")
    @Size(max = 100, message = "ジョブ名は100桁以内で入力してください")
    private String job_name;

    @NotBlank(message = "ユニット完全名を入力してください")
    @Size(max = 500, message = "ユニット完全名は500桁以内で入力してください")
    private String unit_full_name;

    private Date last_exec_datetime;
    private Date next_exec_datetime;

    /**
     * 備考
     */
    @Size(max = 200, message = "備考は200桁以内で入力してください")
    private String note;

    private int update_u_id;
    private Date update_date;
    private boolean delflg;

}