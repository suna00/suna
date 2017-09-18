package net.ion.ice.cjmwave.external.utils;

/**
 * Created by juneyoungoh on 2017. 9. 18..
 */
public class CountryUtils {
    public static String get3Code (String code2) {
        String rtn = "";
        switch (code2.toUpperCase()){
            case "EN":
                rtn = "eng";
                break;
            case "JP":
                rtn = "jpn";
                break;
            case "CH":
                rtn = "zho-cn";
                break;
            case "VN":
                rtn = "vie";
                break;
        }
        return rtn;
    }

    public static String get2Code () {
        return "";
    }
}
