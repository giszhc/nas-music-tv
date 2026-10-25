package com.yunying.music.test;
import android.app.Instrumentation;
import android.os.Bundle;
public class AuthProbe extends Instrumentation {
    public void onCreate(Bundle args){super.onCreate(args);start();}
    public void onStart(){Bundle result=new Bundle();try{result.putString("summary",com.yunying.music.ProtocolChecks.run(getTargetContext()));finish(0,result);}catch(Throwable ex){result.putString("error",ex.toString());finish(1,result);}}
}
