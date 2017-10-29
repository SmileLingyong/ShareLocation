package com.example.smile.shareloaction;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    public LocationClient mLocationClient;
    List<LatLng> points = new ArrayList<LatLng>();	// 位置点集合
    Polyline mPolyline;	// 运动轨迹图层
    LatLng last = new LatLng(0, 0);	// 上一个定位点
    MapStatus.Builder builder;  // 用于构建当前设备点的位置信息
    private TextView positionText;      // 定位显示数据
    private ImageView actionRefersh;    // 更新按钮
    private ImageView actionRefershBg;  // 更新按钮背景
    private ImageView actionStartLocation;
    private StartLocationDialog startLocationDialog;
    private BitmapDescriptor icTraceStart;
    private BitmapDescriptor icTraceEnd;
    private MapView mapView;            // 显示地图的视图（View），它负责从服务端获取地图数据
    private BaiduMap baiduMap;          // BaiduMap类是地图的总控制器
    private BDLocation location;        // 全局BDLocation的引用,// 回调的百度坐标类，内部封装了如经纬度、半径等属性信息
    private MyLocationData locData;     // 当前location位置数据
    private SensorManager mSensorManager;
    private boolean isFirstLocate = true;   //是否首次定位
    private boolean isRequest = false;      //是否点击请求定位按钮
    private int mCurrentDirection = 0;  // 当前设备的方向
    private double mCurrentLat = 0.0;   // 当前设备的经度
    private double mCurrentLon = 0.0;   // 当前设备的纬度
    private double lastX;   // 记录上一次的设备在x轴方向的位置
    private float mCurrentZoom = 18f;   // 默认地图缩放比例

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext()); // 初始化SDK，注意：该条语句一定要放在setContentView()方法调用前
        setContentView(R.layout.activity_main);
        initView();         // 初始化所有控件
        requestPermission();    // 请求获取设备权限
    }

    /**
     *  初始化各种布局文件，并注册获取地址监听器
     */
    private void initView() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);// 获取传感器管理服务

        mapView = (MapView) findViewById(R.id.bmapView);
        baiduMap = mapView.getMap();            // 获取到BaiduMap的实例
        baiduMap.setMyLocationEnabled(true);    // 开启显示用户当前位置在地图上的功能

        positionText = (TextView) findViewById(R.id.position_text_view);
        actionRefersh = (ImageView) findViewById(R.id.btn_action_location);
        actionRefershBg = (ImageView) findViewById(R.id.btn_action_location_bg);
        actionStartLocation = (ImageView) findViewById(R.id.btn_action_start_location);

        icTraceStart = BitmapDescriptorFactory.fromResource(R.drawable.ic_trace_start); // 起点图标
        icTraceEnd = BitmapDescriptorFactory.fromResource(R.drawable.ic_trace_end);     // 终点图标

        // 创建了一个LocationClient的实例，通过调用getApplicationContext()方法来获取一个全局的Context参数并传入。
        // 调用LocationClient的registerLocationListerner()方法来注册一个定位监听器，当获取到位置信息的时候，就会回调这个定位监听器。
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());


        // 更改当前设备定位图标
        // MyLocationConfiguration有两个构造方法，我们调用的是3个参数的构造方法，第一个参数是使用图标类型为普通类型Normal
        // 第二个参数boolean类型,表示是否显示方向信息，第三个参数表示用户自定义的图标，null则表示使用默认图标。
        // 这里我们并没有传入我们设置的图标mCurrentMarker，而将图标参数设置为了null，即使用Normal方式下的默认图标
//        BitmapDescriptor mCurrentMarker = BitmapDescriptorFactory.fromResource(R.drawable.ic_trace_point);
        baiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                com.baidu.mapapi.map.MyLocationConfiguration.LocationMode.NORMAL, true, null));

        actionRefersh.setOnClickListener(this);         // 点击按钮手动请求定位
        actionStartLocation.setOnClickListener(this);   // 共享位置按钮
    }


    /**
     * 请求获取设备权限，当设备权限都获取成功后，再请求开启定位功能
     */
    private void requestPermission() {
        // 创建一个空的List集合，然后依次判断申请的3个权限有没有被授权，如果没有被授权就添加到List集合中，最后将List转换成数组，
        // 再调用ActivityCompat.requestPermissions()方法一次性申请。
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
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        } else {
            requestLocation();
        }
    }

    /**
     * 对权限申请结果的逻辑处理
     */
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


    /**
     * 设置定位的相应参数，并开始定位
     */
    private void requestLocation() {
        // 设置定位的相应参数，如定位更新时间、是否需要获取详细定位信息。
        // 在initLocation()方法中，我们创建了一个LocationClientOption对象，
        // 通过使用 setIsNeedAddress()方法，来开启获得当前位置更详细地址信息
        // 然后调用它的setScanSpan()方法来设置更新的间隔
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(1000);       // 周期性请求定位，1秒返回一次位置
        option.setIsNeedAddress(true);  // 需要获得设备精确位置信息
        option.setCoorType("bd09ll");   // 设置坐标类型，返回百度经纬度坐标系
        option.setOpenGps(true);        // 打开GPS
        option.setNeedDeviceDirect(true);// 获取设备移动方向
        mLocationClient.setLocOption(option);
        mLocationClient.start(); // 通过调用LocationClient的start()方法开始定位功能。
    }


    /**
     * 用于显示设备在地图上，并缩放地图
     * @param location
     */
    private void locateAndZoom(final BDLocation location, LatLng ll, boolean isZoom) {
        // 将地图移动到设备的当前位置
        mCurrentLat = location.getLatitude();
        mCurrentLon = location.getLongitude();
        locData = new MyLocationData.Builder().accuracy(0)
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(mCurrentDirection).latitude(location.getLatitude())
                .longitude(location.getLongitude()).build();
        baiduMap.setMyLocationData(locData);

        // 缩放地图
        builder = new MapStatus.Builder();
        if (isZoom) {   // 缩放地图
            builder.target(ll).zoom(mCurrentZoom);
        } else {        // 不缩放地图
            builder.target(ll);
        }
        baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

    }


    /**
     * 点击具体按钮相应事件
     * @param v
     */
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

    /**
     * 每次方向改变，重新给地图设置定位数据
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[SensorManager.DATA_X];

        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = (int) x;

            if (isFirstLocate) {
                lastX = x;
                return;
            }

            locData = new MyLocationData.Builder().accuracy(0)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(mCurrentLat).longitude(mCurrentLon).build();
            baiduMap.setMyLocationData(locData);
        }
        lastX = x;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * 首次定位很重要，选一个精度相对较高的起始点
     * 注意：如果一直显示gps信号弱，说明过滤的标准过高了，
     你可以将location.getRadius()>25中的过滤半径调大，比如>40，
     并且将连续5个点之间的距离DistanceUtil.getDistance(last, ll ) > 5也调大一点，比如>10，
     这里不是固定死的，你可以根据你的需求调整，如果你的轨迹刚开始效果不是很好，你可以将半径调小，两点之间距离也调小，
     gps的精度半径一般是10-50米
     */
    private LatLng getMostAccuracyLocation(BDLocation location){

        if (location.getRadius() > 40) { // gps位置精度大于40米的点直接弃用
            return null;
        }

        LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());

        if (DistanceUtil.getDistance(last, ll ) > 10) {
            last = ll;
            points.clear(); // 有任意连续两点位置大于10，重新取点
            return null;
        }
        points.add(ll);
        last = ll;
        //有5个连续的点之间的距离小于10，认为gps已稳定，以最新的点为起始点
        if(points.size() >= 5){
            points.clear();
            return ll;
        }
        return null;
    }

    /**
     * 重写 onResume()、onPause()、onDestory() 3个方法，对MapView进行管理，以保证资源能够及时的得到释放。
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        // 为系统的方向传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 取消注册传感器监听
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();     // 停止定位功能
        mapView.getMap().clear();   // 通过getMap()获取地图控制器,然后使用clear()清空地图所有的 Overlay 覆盖物以及 InfoWindow
        mapView.onDestroy();        // 销毁MapView
        mapView = null;             // 并将MapView重置为null
        baiduMap.setMyLocationEnabled(false);   // 程序退出时，关闭将当前设备显示在地图上的功能
        icTraceStart.recycle();     // 回收起点图片的 Bitmap
        icTraceEnd.recycle();       // 回收终点图片的 Bitmap
    }

    /**
     * 定位监听器
     */
    public class MyLocationListener implements BDLocationListener {

        @Override      // 在这个方法中，我们可以获取到丰富的地理位置信息
        public void onReceiveLocation(BDLocation location) {

            if (location == null || mapView == null) {
                return;
            }

            MainActivity.this.location = location;

            StringBuilder currentPosition = new StringBuilder();
            currentPosition.append("纬度：").append(location.getLatitude()).append("\n");
            currentPosition.append("经线：").append(location.getLongitude()).append("\n");
            currentPosition.append("方向：").append(location.getDirection()).append("\n");
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

            // 注意：这里只接受GPS点，需要在室外定位， 自己后来加上了可以使用网络定位
            if (location.getLocType() == BDLocation.TypeGpsLocation || location.getLocType() == BDLocation.TypeNetWorkLocation) {

                // 首次定位
                if (isFirstLocate) {
                    LatLng ll = null;
                    ll = getMostAccuracyLocation(location);
                    if (ll == null) {
                        return;
                    }
                    isFirstLocate = false;
                    points.add(ll); // 将第一个点加入轨迹集合
                    last = ll;

                    // 显示当前定位点，并缩放地图
                    locateAndZoom(location, ll, true);

                    //标记起点图层位置
                    MarkerOptions oStart = new MarkerOptions();// 地图标记覆盖物参数配置类
                    oStart.position(points.get(0));// 覆盖物位置点，第一个点为起点
                    oStart.icon(icTraceStart);  // 设置覆盖物图片
                    baiduMap.addOverlay(oStart); // 在地图上添加此图层

                    return; //画轨迹最少得2个点，首地定位到这里就可以返回了
                }

                if (isRequest) {
                    LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                    MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
                    baiduMap.animateMapStatus(update);  // 将地图移动到我们指定的经纬度上
                    isRequest = false;
                    return;
                }

                // 从第二个点开始
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                // sdk回调gps位置的频率是1秒1个，位置点太近动态画在图上不是很明显，可以设置点之间距离大于为5米才添加到集合中
                // 如果当前定位的点距上次的点有10米远，也不加入轨迹点集之中
                if (DistanceUtil.getDistance(last, ll) < 5 ) {
                    return;
                }
                points.add(ll);//如果要运动完成后画整个轨迹，位置点都在这个集合中

                last = ll;

                // 显示当前定位点，不缩放地图
                locateAndZoom(location, ll, false);
                // 清除上一次轨迹，避免重叠绘画
                mapView.getMap().clear();

                // 起始点图层也会被清除，重新绘画
                MarkerOptions oStart = new MarkerOptions();
                oStart.position(points.get(0));
                oStart.icon(icTraceStart);
                baiduMap.addOverlay(oStart);

                // 将points集合中的点绘制轨迹线条图层，显示在地图上
                OverlayOptions ooPolyline = new PolylineOptions().width(13).color(0xAAFF0000).points(points);
                mPolyline = (Polyline) baiduMap.addOverlay(ooPolyline);
            }

        }
    }

}

