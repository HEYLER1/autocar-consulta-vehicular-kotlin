package com.example.app_autocar;

/** Ordinary light/heavy plate hints, not proof of residence or where fines exist.
 * Source: https://busquedas.elperuano.pe/dispositivo/NL/2475783-1 (Anexo II).
 * Old, special and minor-vehicle formats are not inferred here.
 */
public final class PlateRegion {
    private PlateRegion() {}
    public static String region(String value) {
        String p=PlateUtils.normalize(value);
        if(!p.matches("[A-Z][A-Z0-9]{2}[0-9]{3}"))return "";
        switch(p.charAt(0)) {
            case 'A':case 'B':case 'C':case 'D':case 'F':return "Lima y Callao";
            case 'V':return "Arequipa";
            case 'X':return "Cusco";
            case 'Z':return "Tacna, Moquegua y Puno";
            case 'P':return "Piura";
            case 'M':case 'K':return "Chiclayo, Cajamarca y Chachapoyas";
            case 'S':return "Tarapoto";
            case 'L':return "Iquitos";
            case 'T':return "Trujillo";
            case 'U':return "Pucallpa";
            case 'H':return "Áncash";
            case 'W':return "Huancayo y Huánuco";
            case 'Y':return "Ica";
            case 'I':return "Ayacucho";
            default:return "";
        }
    }
    public static boolean recommended(String plate,String title) {
        String region=region(plate);
        if(region.isEmpty())return false;
        if(region.equals("Lima y Callao"))return title.contains("Lima")||title.contains("Callao")||title.startsWith("ATU");
        String city=title.replace("SATCH ","").replace("SATT ","").replace("SAT ","");
        return region.contains(city);
    }
}
