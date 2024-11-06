package kai9.com.common;

import org.springframework.stereotype.Component;

@Component
public class Kai9comUtils {

    // Java,ReactMakerで利用
    public static String replaceControlledSection(String content, String replaceStr, String startStr, String endStr, boolean isTargetStrLeave) {
        // 改行コード（LF, CRLF）に対応した分割方法
        // コンテンツを改行ごとに分割する
        String[] lines = content.split("\\r?\\n");
        StringBuilder result = new StringBuilder();
        boolean replacing = false; // 置換フラグを初期化

        for (int i = 0; i < lines.length; i++) {
            // 現在の行が制御開始マーカーであるかチェック
            if (lines[i].trim().contains(startStr)) {
                // isTargetStrLeaveがtrueの場合、制御開始行も結果に追加
                if (isTargetStrLeave) {
                    result.append(lines[i]).append("\r\n");
                }
                // 置換を開始
                replacing = true;
            }
            // 現在の行が制御終了マーカーであるかチェック
            else if (lines[i].trim().contains(endStr)) {
                // 置換中であれば、replaceStrで置換
                if (replacing) {
                    // replaceStrが末尾に改行コードを含んでいない場合だけ、改行コードを含める
                    if (!replaceStr.endsWith("\r\n")) {
                        result.append(replaceStr).append("\r\n");
                    } else {
                        result.append(replaceStr);
                    }
                    // 置換を終了
                    replacing = false;
                }
                // isTargetStrLeaveがtrueの場合、制御終了行も結果に追加
                if (isTargetStrLeave) {
                    result.append(lines[i]).append("\r\n");
                }
            }
            // 制御セクション外の行はそのまま結果に追加
            else if (!replacing) {
                result.append(lines[i]).append("\r\n");
            }
        }

        return result.toString();
    }

}