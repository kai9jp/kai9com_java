package kai9.com.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kai9.com.model.AppEnv;

/**
 * 環境マスタ :リポジトリ
 */
public interface AppEnv_Repository extends JpaRepository<AppEnv, Integer> {
}
