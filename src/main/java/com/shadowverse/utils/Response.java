package com.shadowverse.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    private Integer code;
    private String msg;
    private Object data;
    
    public static Response error(String msg,int code){
        System.out.println("Requst Error:"+code + " " + msg);
        return new Response(code,msg,null);
    }
    
    public static Response error(String msg){
        return new Response(500,msg,null);
    }
}
