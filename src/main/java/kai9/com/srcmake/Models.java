package kai9.com.srcmake;

import lombok.AllArgsConstructor;

//Java,ReactMakerで利用
//model作成用
@AllArgsConstructor
public class Models {
    public String columnname;
    public String data_type;
    public boolean is_pk;
    public boolean is_not_null;
    public int MaxLength;
    public String FieldName_J;
    public String special_control_relation;
    public double special_control_min;
    public double special_control_max;
    public String unique_index;
    // WEB用
    public boolean is_mod_admin;
    public boolean is_mod_normal;
    public boolean is_mod_readonly;
    public int MinLength;
    public String default_value;
    public String input_type;
    public String validation_check;
}
