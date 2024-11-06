package kai9.com.srcmake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

//kai9com専用のutil
@Component
public class Kai9ComUtils {

    // Java,ReactMakerで利用
    public static ArrayList<Models> modelList = new ArrayList<Models>();

    // Java,ReactMakerで利用
    public static List<Relations> RelationsList = new ArrayList<>();

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

    // Java, ReactMakerで利用
    public static String replaceContent(
            String content,
            List<Map.Entry<String, String>> replacements,
            String startMarker,
            String endMarker,
            boolean isTargetStrLeave) {

        // パターンを動的に構築
        String regex = String.format("(^[^\\r\\n]*?//\\s*%s[^\\r\\n]*?\\R)(.*?)(^[^\\r\\n]*?//\\s*%s[^\\r\\n]*?\\R)", Pattern.quote(startMarker), Pattern.quote(endMarker));

        // String regex = String.format("(^\\s*//%s\\s*\\R)(.*?)(^\\s*//%s\\s*$)", Pattern.quote(startMarker), Pattern.quote(endMarker));
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();

        // キーのセット数を計算
        Set<String> keySet = new HashSet<>();
        for (Map.Entry<String, String> entry : replacements) {
            keySet.add(entry.getKey());
        }
        int setSize = keySet.size(); // キーの数をセット数とする

        while (matcher.find()) {
            String beforeText = matcher.group(1); // 開始コメント
            String matchedText = matcher.group(2); // 置換対象のテキスト
            String afterText = matcher.group(3); // 終了コメント

            // 置換リストを使ってテキストを更新
            StringBuilder replacementTextBuilder = new StringBuilder();

            if (replacements != null) {
                List<Map<String, String>> replacementPairs = new ArrayList<>();
                Map<String, String> currentPair = new LinkedHashMap<>();
                int currentPairSize = 0;

                // replacementsの各エントリを順に処理
                for (Map.Entry<String, String> entry : replacements) {
                    // currentPairにエントリを追加
                    currentPair.put(entry.getKey(), entry.getValue());
                    currentPairSize++;

                    // currentPairSizeがsetSizeに達したらcurrentPairをreplacementPairsに追加
                    if (currentPairSize == setSize) {
                        replacementPairs.add(new LinkedHashMap<>(currentPair)); // currentPairのコピーを追加
                        currentPair.clear(); // currentPairをクリア
                        currentPairSize = 0; // currentPairSizeをリセット
                    }
                }

                // 残ったエントリがある場合、それを追加
                if (!currentPair.isEmpty()) {
                    replacementPairs.add(new LinkedHashMap<>(currentPair));
                }

                for (Map<String, String> replacement : replacementPairs) {
                    String replacedText = matchedText;
                    for (Map.Entry<String, String> entry : replacement.entrySet()) {
                        // 任意のキーと値のペアに基づいて置換
                        replacedText = replacedText.replace(entry.getKey(), entry.getValue());
                    }
                    // 置換されたテキストを追加
                    replacementTextBuilder.append(replacedText);
                }
            }

            String replacementText = replacementTextBuilder.toString();

            if (isTargetStrLeave) {
                // 元のテキストを残して置換
                matcher.appendReplacement(result, Matcher.quoteReplacement(beforeText + replacementTextBuilder.toString() + afterText));
            } else {
                // 元のテキストを削除して置換
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacementText));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 指定されたタグを含む行を文字列から削除します。
     *
     * @param input 入力文字列
     * @param tag 削除対象のタグ
     * @return タグを含む行が削除された文字列
     */
    public static String removeLinesContainingTag(String input, String tag) {
        // タグを含む行を削除
        return input.replaceAll("(?m)^.*" + Pattern.quote(tag) + ".*$[\r\n]*", "");
    }

    // カラム名がシステムカラムの場合はtrueを返す
    public static Boolean isSystemColumn(String columnName) {
        if (columnName.toUpperCase().equals("MODIFY_COUNT")) return true;
        if (columnName.toUpperCase().equals("UPDATE_U_ID")) return true;
        if (columnName.toUpperCase().equals("UPDATE_DATE")) return true;
        if (columnName.toUpperCase().equals("DELFLG")) return true;
        return false;
    }

}
