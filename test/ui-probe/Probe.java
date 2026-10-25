package com.yunying.music.test;
import android.app.Instrumentation;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import android.graphics.Rect;
import org.xmlpull.v1.XmlSerializer;
import java.io.StringWriter;
public class Probe extends Instrumentation {
    private Bundle args;
    public void onCreate(Bundle args) { super.onCreate(args); this.args=args; start(); }
    public void onStart() {
        Bundle result=new Bundle();
        try {
            AccessibilityNodeInfo root=null;
            for(int attempt=0;attempt<20 && root==null;attempt++){android.os.SystemClock.sleep(50);root=getUiAutomation().getRootInActiveWindow();}
            if(root!=null && args!=null && args.containsKey("focus")) {
                focus(root,args.getString("focus"));android.os.SystemClock.sleep(120);root=getUiAutomation().getRootInActiveWindow();
            }
            if(root==null)throw new Exception("No active window");
            if(args!=null&&args.containsKey("setText")){setText(root,args.getString("setText"),args.getString("value",""));android.os.SystemClock.sleep(100);root=getUiAutomation().getRootInActiveWindow();}
            StringWriter output=new StringWriter();XmlSerializer xml=android.util.Xml.newSerializer();xml.setOutput(output);xml.startTag(null,"hierarchy");write(xml,root);xml.endTag(null,"hierarchy");xml.flush();result.putString("xml",output.toString());finish(0,result);
        } catch(Exception ex) {result.putString("error",ex.toString());finish(1,result);}
    }
    private boolean setText(AccessibilityNodeInfo node,String match,String value){
        if("android.widget.EditText".contentEquals(node.getClassName())&&(match.isEmpty()||match.contentEquals(node.getText()==null?"":node.getText()))){Bundle data=new Bundle();data.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,data);}
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null&&setText(child,match,value))return true;}return false;
    }
    private boolean focus(AccessibilityNodeInfo node,String label) {
        if(label.contentEquals(node.getText()==null?"":node.getText()) || label.contentEquals(node.getContentDescription()==null?"":node.getContentDescription())) {
            AccessibilityNodeInfo current=node;
            while(current!=null && !current.isFocusable())current=current.getParent();
            if(current!=null)return current.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        }
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null&&focus(child,label))return true;}
        return false;
    }
    private void write(XmlSerializer xml,AccessibilityNodeInfo node)throws Exception {
        xml.startTag(null,"node");
        xml.attribute(null,"text",node.getText()==null?"":node.getText().toString());
        xml.attribute(null,"content-desc",node.getContentDescription()==null?"":node.getContentDescription().toString());
        xml.attribute(null,"class",node.getClassName()==null?"":node.getClassName().toString());
        xml.attribute(null,"selected",Boolean.toString(node.isSelected()));xml.attribute(null,"enabled",Boolean.toString(node.isEnabled()));xml.attribute(null,"clickable",Boolean.toString(node.isClickable()));xml.attribute(null,"focused",Boolean.toString(node.isFocused()));
        Rect r=new Rect();node.getBoundsInScreen(r);xml.attribute(null,"bounds","["+r.left+","+r.top+"]["+r.right+","+r.bottom+"]");
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null){write(xml,child);child.recycle();}}
        xml.endTag(null,"node");
    }
}
