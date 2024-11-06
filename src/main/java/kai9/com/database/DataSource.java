package kai9.com.database;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

import lombok.Getter;
import lombok.Setter;

//データソースを定義するクラス
//Springアプリケーションに複数のデータソースを定義するので、そのプライマリデータソースを指定
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.datasource.primary") // application.propertiesからプロパティを自動的に注入するためのアノテーション
public class DataSource {

    // データソースに必要なプロパティ
    private String driverClassName; // ドライバークラス名
    private String url; // データベースのURL
    private String username; // ユーザー名
    private String password; // パスワード
    private int maximumPoolSize; // HikariCPの最大プールサイズ
    private int minimumIdle; // HikariCPの最小アイドル接続数

    // プライマリデータソースを定義するためのメソッド
    @Bean
    @Primary // プライマリデータソースであることを示すアノテーション
    public javax.sql.DataSource primaryDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(maximumPoolSize); // HikariCPの最大プールサイズを設定
        dataSource.setMinimumIdle(minimumIdle); // HikariCPの最小アイドル接続数を設定
        return dataSource;
    }

    // プライマリデータソースを使用するためのJdbcTemplateを作成するためのメソッド
    @Bean
    @Primary // プライマリデータソースであることを示すアノテーション
    public JdbcTemplate createJdbcTemplate(javax.sql.DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
