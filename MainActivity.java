package com.example.mydiary;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;


import com.example.mydiary.room.Diary;
import com.example.mydiary.room.DiaryDao;
import com.example.mydiary.room.DiaryDatabase;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String name;

    private Diary diary;
    private DiaryDao diaryDao;
    private DiaryDatabase diaryDatabase;
    //要删除的项的标志HashMap数组
    HashMap<Integer,String> str=new HashMap<>();
    //设置是否在onStart中更新数据源的标志
    private int update=0;

    //存储diary对象的数组
    private List<Diary> diaryList=new ArrayList<>();
    //适配器
    private diaryAdapter adapter;
    //是否处于多选删除状态
    // 设置这个变量是为了让区分正常点击和多选删除时的点击事件
    //以及长按状态时不再响应长按事件
    private boolean isDeleteState=false;
    //网格布局管理器
    GridLayoutManager gridLayoutManager=new GridLayoutManager(this,1);
    //线性布局管理器
    LinearLayoutManager linearLayoutManager=new LinearLayoutManager(this);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent=getIntent();
        name =intent.getStringExtra("name");
//        dbEngine=new DBEngine(this);
//        //从数据库中获取日记
//        diaryList =dbEngine.querAllDiaries();
        diaryDatabase = Room.databaseBuilder(getApplicationContext(),
                DiaryDatabase.class, "diary").allowMainThreadQueries().build();
        diaryDao= diaryDatabase.getDiaryDao();
        List<Diary> diaryList= diaryDao.getContentByauthor(name);

        RecyclerView recyclerView=(RecyclerView)findViewById(R.id.recycler_view);
        if(diaryList.size()>0){
            //数据库中有日记记录，以网格布局展示
            recyclerView.setLayoutManager(gridLayoutManager);
        }else{
            //数据库中没有日记记录，用线性布局显示“无数据”
            recyclerView.setLayoutManager(linearLayoutManager);
        }
        //适配器的初始化，第一个参数传入数据源，第二个参数false表示正常状态;true表示多选删除状态
        adapter=new diaryAdapter(diaryList,false);
        recyclerView.setAdapter(adapter);
        //初始化‘新建’和‘删除’按钮
        Button build=(Button)findViewById(R.id.build_button);
        Button delete=(Button)findViewById(R.id.delete_button);
        //点击‘新建’按钮时，跳转到编辑日记的活动
        build.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this, EditActivity.class);
                intent.putExtra("diaryTitle","");//传入标题内容，这里为空
                intent.putExtra("diaryContent","");//传入日记内容，这里为空
                intent.putExtra("name",name);
                intent.putExtra("signal",0);//传入‘新建’标志：0
                startActivity(intent);
            }
        });

        //点击‘删除按钮时，执行删除操作
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelections();
            }
        });
        //为RecyclerView添加点击事件响应和长按事件响应
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
            //点击事件响应
            @Override
            public void onItemClick(View view, int position) {
                if(!isDeleteState&&diaryList.size()>0){

                    // 普通点击事件
                    Diary mDiary=diaryList.get(position);
                    String diaryContent=mDiary.getContent().toString();
                    String diaryTime=mDiary.getTime().toString();
                    String title=mDiary.getTitle().toString();
                   // Log.d("xxx", diaryContent);
                   // Log.d("xxx", diaryContent);

                    Intent intent=new Intent(MainActivity.this,EditActivity.class);
                    intent.putExtra("diaryContent",diaryContent);
                    intent.putExtra("diaryTitle",title);
                    intent.putExtra("name",name);

                    //传入修改标志1：表示修改原有日记内容
                    intent.putExtra("signal",1);
                    //传递内容

                    startActivity(intent);
                }else if (diaryList.size()>0){
                    //长按进入多选状态后的点击事件
                    CheckBox checkBox = (CheckBox) view.findViewById(R.id.check_box);
                    if (checkBox.isChecked()) {
                        checkBox.setChecked(false);
                        str.remove(position);
                    } else {
                        str.put(position,diaryAdapter.mDiaryList.get(position).getContent());
                        checkBox.setChecked(true);
                    }
                }
            }
            @Override
            public void onItemLongClick(View view, int position) {
                // 长按事件
                if(!isDeleteState&&diaryList.size()>0){
                    isDeleteState=true;
                    str.clear();
                    //把当前选中的的复选框设置为选中状态
                    diaryAdapter.isSelected.put(position,true);
                    //把所有的CheckBox显示出来
                    RecyclerView recyclerView=(RecyclerView)findViewById(R.id.recycler_view);
                    //第二个参数为true表示长按进入多选删除状态时的适配器初始化
                    List<Diary> diaryList1= diaryDao.getContentByauthor(name);
                    adapter=new diaryAdapter(diaryList1,true);
                    recyclerView.setAdapter(adapter);
                    str.put(position,diaryAdapter.mDiaryList.get(position).getContent());
                }
            }

        }));

    }

    //用户返回该活动时
    @Override
    protected void onStart() {
        super.onStart();
        isDeleteState=false;
        diaryList=new ArrayList<>();

        diaryList.clear();
        RecyclerView recyclerView=(RecyclerView)findViewById(R.id.recycler_view);
        List<Diary> data= diaryDao.getContentByauthor(name);
        for(Diary mdiary:data){
            diaryList.add(mdiary);
        }
        if(diaryList.size()>0){
            recyclerView.setLayoutManager(gridLayoutManager);
        }else{
            recyclerView.setLayoutManager(linearLayoutManager);
        }
        adapter=new diaryAdapter(diaryList,false);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        update=1;//更新数据源
    }

    //执行删除的函数
    private void deleteSelections() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (str.size()==0) {
            builder.setTitle("提示").setMessage("当前未选中项目").setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    RecyclerView recyclerView=(RecyclerView)findViewById(R.id.recycler_view);
                    adapter=new diaryAdapter(diaryList,false);
                    recyclerView.setAdapter(adapter);
                    isDeleteState=false;
                    str.clear();
                }
            }).create().show();
        } else {
            builder.setTitle("提示");
            builder.setMessage("确认删除所选日记？");
            builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    for(int i:str.keySet()){
                        //DataSupport.deleteAll(diary.class,"content=?",str.get(i));
                        Diary diary= diaryDao.getUserByName(str.get(i));
                        diaryDao.deleteDiary(diary);
                    }
                    diaryList.clear();
                    List<Diary> data= diaryDao.getContentByauthor(name);//=DataSupport.findAll(diary.class);
                    for(Diary mdiary:data){
                        diaryList.add(mdiary);
                    }
                    adapter.notifyDataSetChanged();
                    RecyclerView recyclerView=(RecyclerView)findViewById(R.id.recycler_view);
                    if(diaryList.size()>0){
                        recyclerView.setLayoutManager(gridLayoutManager);
                    }else{
                        recyclerView.setLayoutManager(linearLayoutManager);
                    }
                    adapter=new diaryAdapter(diaryList,false);
                    recyclerView.setAdapter(adapter);
                    isDeleteState=false;
                    str.clear();
                    Toast.makeText(MainActivity.this,"删除成功！",Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    RecyclerView recyclerView=(RecyclerView)findViewById(R.id.recycler_view);
                    adapter=new diaryAdapter(diaryList,false);
                    recyclerView.setAdapter(adapter);
                    isDeleteState=false;
                    str.clear();
                }
            });
            builder.create().show();
        }
    }

}