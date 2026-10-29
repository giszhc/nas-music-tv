package com.yunying.music;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.database.Cursor;
import android.widget.TextView;
import java.util.*;
/** 系统目录授权，无需全盘文件权限；保留 URI 授权供后续播放。 */
public final class LocalImportActivity extends Activity {
 private TextView status;
 public void onCreate(Bundle state){super.onCreate(state);Ui.immersive(this);status=Ui.text(this,"请选择本地音乐目录",20,Ui.TEXT,false);status.setGravity(17);status.setBackground(new ImmersiveBackground());setContentView(status);if(state==null){try{Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);startActivityForResult(picker,10);}catch(android.content.ActivityNotFoundException ex){android.widget.Toast.makeText(this,"设备没有可用的系统目录选择器",1).show();finish();}}}
 protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null){finish();return;}final Uri tree=data.getData();try{getContentResolver().takePersistableUriPermission(tree,data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ex){status.setText("无法保存目录访问权限，请重新选择目录");return;}status.setText("正在扫描目录及子目录…");new Thread(()->{try{List<Entry> songs=new ArrayList<>();Deque<String> pending=new ArrayDeque<>();Set<String> visited=new HashSet<>();pending.add(DocumentsContract.getTreeDocumentId(tree));while(!pending.isEmpty()){String id=pending.removeFirst();if(!visited.add(id))continue;Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,id);try(Cursor cursor=getContentResolver().query(children,new String[]{"document_id","_display_name","mime_type","_size"},null,null,null)){if(cursor==null)throw new java.io.IOException("目录无法读取");while(cursor.moveToNext()){String child=cursor.getString(0);if(DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2)))pending.add(child);else{Entry e=new Entry(cursor.getString(1),false,Entry.SRC_LOCAL);e.path=DocumentsContract.buildDocumentUriUsingTree(tree,child).toString();e.size=cursor.getLong(3);if(e.isAudio())songs.add(e);}}}}int count=MusicLibrary.addToPlaylist(this,songs);runOnUiThread(()->{android.widget.Toast.makeText(this,"已导入 "+count+" 首，跳过重复 "+(songs.size()-count)+" 首",1).show();finish();});}catch(Exception ex){runOnUiThread(()->status.setText("导入失败："+ex.getMessage()+"，按返回键重试"));}},"local-import").start();}
}
