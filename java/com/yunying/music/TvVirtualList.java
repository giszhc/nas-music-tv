package com.yunying.music;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

/** 原生 GridView 的可见项回收：焦点留在列表，方向键只移动数据位置，不重建整表。 */
final class TvVirtualList extends FrameLayout {
    interface Renderer { View bind(int position, View recycled); }
    interface PositionAction { void run(int position); }
    private final AbsListView grid;
    private final int count, columns;
    private PositionAction selected, clicked;
    private Runnable up, right,bottom,left;
    private int position;
    TvVirtualList(Context context,int count,int columns,int height,int gap,Renderer renderer) {
        super(context);this.count=count;this.columns=columns;
        setClipChildren(true);setClipToPadding(true);
        if(columns==1){ListView list=new ListView(context);list.setItemsCanFocus(false);list.setDivider(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));list.setDividerHeight(Ui.dp(context,gap));grid=list;}
        else{GridView list=new GridView(context);list.setNumColumns(columns);list.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);list.setGravity(Gravity.TOP);list.setVerticalSpacing(Ui.dp(context,gap));list.setHorizontalSpacing(Ui.dp(context,24));grid=list;}
        grid.setPadding(Ui.dp(context,20),Ui.dp(context,12),Ui.dp(context,20),Ui.dp(context,40));
        grid.setClipChildren(false);grid.setClipToPadding(false);grid.setSelector(android.R.color.transparent);grid.setCacheColorHint(Color.TRANSPARENT);
        grid.setVerticalScrollBarEnabled(false);grid.setOverScrollMode(OVER_SCROLL_NEVER);grid.setFadingEdgeLength(0);
        grid.setFocusable(true);grid.setFocusableInTouchMode(true);grid.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        if(android.os.Build.VERSION.SDK_INT>=26)grid.setDefaultFocusHighlightEnabled(false);
        addView(grid,new FrameLayout.LayoutParams(-1,-1));
        grid.setAdapter(new BaseAdapter(){
            public int getCount(){return count;} public Object getItem(int p){return p;} public long getItemId(int p){return p;}
            public View getView(int p,View recycled,ViewGroup parent){
                FrameLayout holder=recycled instanceof FrameLayout?(FrameLayout)recycled:new FrameLayout(context);
                View old=holder.getChildCount()==0?null:holder.getChildAt(0);
                Object oldPosition=holder.getTag();
                if(old!=null&&(!(oldPosition instanceof Integer)||(Integer)oldPosition!=p)){
                    old.animate().cancel();old.setScaleX(1);old.setScaleY(1);holder.setTranslationZ(0);holder.setTag(R.id.virtual_row_active,null);
                    if(old.getBackground()instanceof GlassDrawable)((GlassDrawable)old.getBackground()).setFocus(false,false);colors(old,false);
                }
                holder.setClipChildren(false);holder.setClipToPadding(false);View content=renderer.bind(p,old);
                if(content!=old){holder.setTag(R.id.virtual_row_active,null);holder.removeAllViews();holder.addView(content,new FrameLayout.LayoutParams(-1,-1));}
                blockFocus(content);holder.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);content.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);holder.setTag(p);holder.setLayoutParams(new AbsListView.LayoutParams(-1,Ui.dp(context,height)));
                paint(holder,grid.hasFocus()&&p==grid.getSelectedItemPosition());return holder;
            }
        });
        grid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int p,long id){position=p;refresh();if(selected!=null)selected.run(p);}
            public void onNothingSelected(AdapterView<?> parent){}
        });
        grid.setOnItemClickListener((parent,view,p,id)->{position=p;if(clicked!=null)clicked.run(p);});
        grid.setOnFocusChangeListener((view,focus)->refresh());
        grid.setOnScrollListener(new AbsListView.OnScrollListener(){public void onScrollStateChanged(AbsListView v,int state){}public void onScroll(AbsListView v,int first,int visible,int total){refresh();}});
        grid.setOnKeyListener((view,key,event)->{
            int p=Math.max(0,grid.getSelectedItemPosition());
            if(key==KeyEvent.KEYCODE_DPAD_UP&&p<columns&&up!=null){if(event.getAction()==KeyEvent.ACTION_DOWN)up.run();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_LEFT&&p%columns==0&&left!=null){if(event.getAction()==KeyEvent.ACTION_DOWN)left.run();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_RIGHT&&right!=null){if(event.getAction()==KeyEvent.ACTION_DOWN)right.run();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_DOWN&&p+columns>=count&&bottom!=null){if(event.getAction()==KeyEvent.ACTION_DOWN)bottom.run();return true;}
            // 数据边界停住；禁止焦点逃到页面其他控件再从顶部进入。
            if(key==KeyEvent.KEYCODE_DPAD_DOWN&&p+columns>=count||key==KeyEvent.KEYCODE_DPAD_UP&&p<columns||key==KeyEvent.KEYCODE_DPAD_LEFT&&p%columns==0||key==KeyEvent.KEYCODE_DPAD_RIGHT&&p%columns==columns-1)return true;
            if(event.getAction()==KeyEvent.ACTION_DOWN&&Ui.motionEnabled(context)){
                int delta=key==KeyEvent.KEYCODE_DPAD_DOWN?columns:key==KeyEvent.KEYCODE_DPAD_UP?-columns:key==KeyEvent.KEYCODE_DPAD_RIGHT?1:key==KeyEvent.KEYCODE_DPAD_LEFT?-1:0;
                int next=p+delta;
                if(delta!=0&&next>=0&&next<count){grid.setSelection(next);grid.smoothScrollToPosition(next);return true;}
            }
            return false;
        });
        refresh();
    }
    TvVirtualList selected(PositionAction action){selected=action;return this;}
    TvVirtualList clicked(PositionAction action){clicked=action;return this;}
    TvVirtualList up(Runnable action){up=action;return this;}
    TvVirtualList right(Runnable action){right=action;return this;}
    TvVirtualList left(Runnable action){left=action;return this;}
    android.os.Parcelable snapshot(){return grid.onSaveInstanceState();}
    void restore(android.os.Parcelable state){grid.requestFocus();grid.onRestoreInstanceState(state);post(()->refresh());}
    TvVirtualList bottom(Runnable action){bottom=action;return this;}
    int position(){int p=grid.getSelectedItemPosition();return p<0?position:p;}
    void focusPosition(int p){final int target=Math.max(0,Math.min(count-1,p));position=target;grid.requestFocus();grid.setSelection(target);post(()->{grid.setSelection(target);refresh();});}
    private void refresh(){
        int selectedPosition=grid.getSelectedItemPosition();
        String description="虚拟列表 · 第 "+(count==0?0:Math.max(0,selectedPosition)+1)+" / "+count+" 项";if(!description.contentEquals(grid.getContentDescription()==null?"":grid.getContentDescription()))grid.setContentDescription(description);
        for(int i=0;i<grid.getChildCount();i++){View child=grid.getChildAt(i);Object tag=child.getTag();paint((FrameLayout)child,grid.hasFocus()&&tag instanceof Integer&&(Integer)tag==selectedPosition);}
    }
    private static void blockFocus(View view){view.setFocusable(false);view.setClickable(false);if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)blockFocus(group.getChildAt(i));}}
    private static void paint(FrameLayout holder,boolean active){
        if(holder.getChildCount()==0)return;Object prior=holder.getTag(R.id.virtual_row_active);if(prior instanceof Boolean&&(Boolean)prior==active)return;holder.setTag(R.id.virtual_row_active,active);
        View content=holder.getChildAt(0);GlassDrawable background;
        if(content.getBackground()instanceof GlassDrawable)background=(GlassDrawable)content.getBackground();else{background=new GlassDrawable(content.getContext(),12,true);content.setBackground(background);}
        boolean animate=holder.isAttachedToWindow()&&Ui.motionEnabled(content.getContext());background.setFocus(active,animate);colors(content,active);
        content.animate().cancel();float scale=active?1.045f:1;
        // 仅变换可见卡片，左右20单位留白容纳放大；不改变行高和数据位置。
        if(animate)content.animate().scaleX(scale).scaleY(scale).setDuration(250).setInterpolator(Ui.EASE).start();else{content.setScaleX(scale);content.setScaleY(scale);}
        holder.setTranslationZ(active?Ui.dp(content.getContext(),4):0);
    }
    private static void colors(View view,boolean active){
        if(view instanceof TextView){TextView text=(TextView)view;if(text.getTag(com.yunying.music.R.id.virtual_text_color)==null)text.setTag(com.yunying.music.R.id.virtual_text_color,text.getCurrentTextColor());int normal=(Integer)text.getTag(com.yunying.music.R.id.virtual_text_color);text.setTextColor(active?(normal==Ui.TEXT2?Ui.TEXT2_ON_LIGHT:Ui.TEXT_ON_LIGHT):normal);}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)colors(group.getChildAt(i),active);}
    }
}
