package ekstraklasa.predictor.constant;

import java.util.Map;

/**
 * @author Witold Drożdżowski (drozdzowski.witold@gmail.com)
 * @since 02.2026
 */
public interface FootballClubValueConstants {

    Map<String, Integer> CLUB_MARKET_VALUE_EUR = Map.ofEntries(
            Map.entry("lech_poznan", 42_500_000),
            Map.entry("rakow_czestochowa", 42_300_000),
            Map.entry("widzew_lodz", 40_900_000),
            Map.entry("legia_warszawa", 32_780_000),
            Map.entry("jagiellonia_bialystok", 32_350_000),
            Map.entry("cracovia", 23_150_000),
            Map.entry("lechia_gdansk", 21_150_000),
            Map.entry("gornik_zabrze", 20_080_000),
            Map.entry("pogon_szczecin", 19_330_000),
            Map.entry("zaglebie_lubin", 15_800_000),
            Map.entry("korona_kielce", 15_680_000),
            Map.entry("radomiak_radom", 14_130_000),
            Map.entry("motor_lublin", 12_980_000),
            Map.entry("wisla_plock", 11_400_000),
            Map.entry("gks_katowice", 9_330_000),
            Map.entry("piast_gliwice", 8_150_000),
            Map.entry("bruk_bet_termalica_nieciecza", 7_850_000),
            Map.entry("arka_gdynia", 7_600_000)
    );

}
