package com.example.smile.shareloaction;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public LocationClient mLocationClient;
    private TextView positionText;      // 定位显示数据
    private ImageView actionRefersh;    // 更新按钮
    private ImageView actionRefershBg;  // 更新按钮背景
    private ImageView actionStartLocation;
    private StartLocationDialog startLocationDialog;

    private MapView mapView;            // 显示地图的视图
    private BaiduMap baiduMap;          // BaiduMap类是地图的总控制器
    private BDLocation location;        // 全局BDLocation的引用

    private boolean isFirstLocate = true;   //是否首次定位
    private boolean isRequest = false;      //是否点击请求定位按钮


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 创建了一个LocationClient的实例，通过调用getApplicationContext()方法来获取一个全局的Context参数并传入。
        // 调用LocationClient的registerLocationListerner()方法来注册一个定位监听器，当获取到位置信息的时候，就会回调这个定位监听器。
        // 创建一个空的List集合，然后依次判断申请的3个权限有没有被授权，如果没有被授权就添加到List集合中，最后将List转换成数组，
        // 再调用ActivityCompat.requestPermissions()方法一次性申请。
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        SDKInitializer.initialize(getApplicationContext()); // 初始化SDK，注意：该条语句一定要放在setContentView()方法调用前

        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.bmapView);
        baiduMap = mapView.getMap();            // 获取到BaiduMap的实例
        baiduMap.setMyLocationEnabled(true);    // 开启显示用户当前位置在地图上的功能
        positionText = (TextView) findViewById(R.id.position_text_view);
        actionRefersh = (ImageView) findViewById(R.id.btn_action_location);
        actionRefershBg = (ImageView) findViewById(R.id.btn_action_location_bg);
        actionStartLocation = (ImageView) findViewById(R.id.btn_action_start_location);

        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        } else {
            requestLocation();
        }

        // 点击按钮手动请求定位
        actionRefersh.setOnClickListener(this);
        actionStartLocation.setOnClickListener(this);

        // 更改当前设备定位图标
        BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromResource(R.drawable.icon_point);
        MyLocationConfiguration configuration =new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL,true,null);
        baiduMap.setMyLocationConfigeration(configuration);

    }


    // 该函数功能：用于显示设备当前位置于地图上
    private void navigateTo(BDLocation location) {
        // 第一次启动时，显示当前设备位置
        if (isFirstLocate) {
            // 获取经纬度信息，并将其存入LatLng对象之中，然后调用MapStatusUpdateFactory的newLatLng()方法将LatLng对象传入。
            // 接着将返回的MapStatusUpdate对象，作为参数传入到BaiduMap的animateMapStatus()方法当中。
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(update);
            // 通过调用MapStatusUpdateFactory的zoomTo()方法，设置缩放级别，返回一个MapStatusUpdate对象
            // 并将其作为参数传入到BaiduMap的animateMapStatus()方法当中，实现地图的缩放。
            update = MapStatusUpdateFactory.zoomTo(21f);
            baiduMap.animateMapStatus(update);
            // isFirstLocate 该变量是为了防止多次调用animateMapStatus()方法，
            // 因为将地图移动到我们当前位置只需要在程序第一次定位的时候调用一次就可以了
            isFirstLocate = false;
        }

        // 点击请求按钮时做的处理
        if (isRequest) {
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(update);
            isRequest = false;
        }

        // MyLocationData.Builder类是用来封装设备当前所在位置的，把要封装的信息都设置完毕后，
        // 调用build()方法，会返回一个MyLocationData实例。
        // 然后再将该实例传入到BaiduMap的setMyLocationData()方法中，就可以让设备当前位置显示在地图上了
        MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
        locationBuilder.latitude(location.getLatitude());
        locationBuilder.longitude(location.getLongitude());
        MyLocationData locationData = locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
    }


    //开始定位
    private void requestLocation() {
        initLocation(); // 设置定位的相应参数，如定位更新时间、是否需要获取详细定位信息。
        mLocationClient.start(); // 通过调用LocationClient的start()方法开始定位功能。
    }


    // 在initLocation()方法中，我们创建了一个LocationClientOption对象，
    // 通过使用 setIsNeedAddress()方法，来开启获得当前位置更详细地址信息
    // 然后调用它的setScanSpan()方法来设置更新的间隔
    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(1000);
        option.setIsNeedAddress(true);
        option.setCoorType("bd09ll");  // 设置坐标类型，返回百度经纬度坐标系
        mLocationClient.setLocOption(option);
        // 显示当前获取位置类型
//        Toast.makeText(this, option.getCoorType(), Toast.LENGTH_SHORT).show();
    }


    //对权限申请结果的逻辑处理
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意所有权限才能使用本程序", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }


    // 重写 onResume()、onPause()、onDestory() 3个方法，对MapView进行管理，以保证资源能够及时的得到释放。
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }

    // 点击具体按钮响应事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_action_location:
                Toast.makeText(MainActivity.this, "定位到当前位置", Toast.LENGTH_SHORT).show();
                // 每当点击一次按钮，就将isRequest 改为true，以便后面调用navigateTo()方法，将地图移动到当前设备位置。
                isRequest = true;
                requestLocation();
                break;

            case R.id.btn_action_start_location:
                // 实例化startLocationDialog
                startLocationDialog = new StartLocationDialog(MainActivity.this);
                startLocationDialog.showAtLocation(mapView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                break;

            default:
                break;
        }

    }

    public class MyLocationListener implements BDLocationListener {

        @Override      // 在这个方法中，我们可以获取到丰富的地理位置信息
        public void onReceiveLocation(BDLocation location) {
            if (location == null) {
                return;
            }

            MainActivity.this.location = location;

            StringBuilder currentPosition = new StringBuilder();
            currentPosition.append("纬度：").append(location.getLatitude()).append("\n");
            currentPosition.append("经线：").append(location.getLongitude()).append("\n");
            currentPosition.append("国家：").append(location.getCountry()).append("\n");
            currentPosition.append("省：").append(location.getProvince()).append("\n");
            currentPosition.append("市：").append(location.getCity()).append("\n");
            currentPosition.append("区：").append(location.getDistrict()).append("\n");
            currentPosition.append("街道：").append(location.getStreet()).append("\n");
            currentPosition.append("定位方式：");
            if (location.getLocType() == BDLocation.TypeGpsLocation) {
                currentPosition.append("GPS");
            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
                currentPosition.append("网络");
            }

            // 将当前获取到的设备位置信息显示在 TextView上
            positionText.setText(currentPosition);
            // 调用navigateTo()函数，将设备显示于地图之上
            if (location.getLocType() == BDLocation.TypeGpsLocation || location.getLocType() == BDLocation.TypeNetWorkLocation) {
                navigateTo(location);
            }

        }
    }



}

