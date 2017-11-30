package com.story.happyjie.swipemenulayouttest;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.story.happyjie.smartswipemenulayout.SmartSwipeMenuLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView listView;
    private MyAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        mAdapter = new MyAdapter(this, generateTestData());
        listView.setAdapter(mAdapter);
    }

    private List<String> generateTestData(){
        List<String> list = new ArrayList<>();
        for(int i = 0; i < 20; i++){
            list.add("上岛咖啡经适房的"+ (i+1));
        }
        return list;
    }

    private class MyAdapter extends BaseAdapter{

        private List<String> dates;
        private Context context;

        public MyAdapter(Context context, List<String> dates) {
            this.dates = dates;
            this.context = context;
        }

        @Override
        public int getCount() {
            return dates.size();
        }

        @Override
        public Object getItem(int position) {
            return dates.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if(null == convertView){
                convertView = LayoutInflater.from(context).inflate(R.layout.item_test, null);
                holder = new ViewHolder();
                holder.tvContent = (TextView) convertView.findViewById(R.id.tv_content);
                holder.tvleftMenu = (TextView) convertView.findViewById(R.id.left_menu_1);
                holder.tvRightMenu1 = (TextView) convertView.findViewById(R.id.right_menu_1);
                holder.tvRightMenu2 = (TextView) convertView.findViewById(R.id.right_menu_2);
                holder.itemLayout = (SmartSwipeMenuLayout) convertView.findViewById(R.id.smartSwipeMenuLayout);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.tvContent.setText((String)getItem(position));

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context,((TextView) v).getText()+(String)getItem(position), Toast.LENGTH_SHORT).show();
                    holder.itemLayout.resetStatus();
                }
            };

            holder.tvleftMenu.setOnClickListener(listener);
            holder.tvRightMenu1.setOnClickListener(listener);
            holder.tvRightMenu2.setOnClickListener(listener);

            return convertView;
        }

        class ViewHolder{
            TextView tvContent, tvleftMenu, tvRightMenu1, tvRightMenu2;
            SmartSwipeMenuLayout itemLayout;
        }
    }
}
