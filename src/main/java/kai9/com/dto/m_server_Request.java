package kai9.com.dto;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Data;

/**
 * サーバー情報 リクエストデータ
 */
@Data
@SuppressWarnings("serial")
public class m_server_Request implements Serializable {

    /**
     * サーバID
     */
    private int server_id;

    /**
     * 更新回数
     */
    private int modify_count;

    /**
     * ホスト名
     */
    @NotBlank(message = "ホスト名を入力してください")
    @Size(max = 20, message = "ホスト名は20桁以内で入力してください")
    private String host_name;

    /**
     * 利用端末IPアドレス
     * https://qiita.com/BRSF/items/2a83af4e605019e8c2fe 正規表現で値なしor条件付き入力の制御
     */
    @Size(max = 15, message = "利用端末IPアドレスは15桁以内で入力してください")
    @Pattern(regexp = "^((([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]).){3}([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))?$", message = "IPアドレスとして正しい形式にしてください")
    private String ip;

    /**
     * 備考
     */
    @Size(max = 200, message = "備考は200桁以内で入力してください")
    private String note;

    private int update_u_id;
    private Date update_date;
    private boolean delflg;

}