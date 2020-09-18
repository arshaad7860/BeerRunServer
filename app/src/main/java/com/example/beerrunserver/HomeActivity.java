package com.example.beerrunserver;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.example.beerrunserver.Adapter.PdfDocumentAdapter;
import com.example.beerrunserver.Common.Common;
import com.example.beerrunserver.Common.PDFUtils;
import com.example.beerrunserver.EventBus.CategoryClick;
import com.example.beerrunserver.EventBus.ChangeMenuClick;
import com.example.beerrunserver.EventBus.PrintOrderEvent;
import com.example.beerrunserver.EventBus.ToastEvent;
import com.example.beerrunserver.Model.FCMResponse;
import com.example.beerrunserver.Model.FCMSendData;
import com.example.beerrunserver.Model.OrderModel;
import com.example.beerrunserver.Remote.RetrofitFCMClient;
import com.example.beerrunserver.Services.IFCMService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PICK_IMAGE_REQUEST = 7171;
    private AppBarConfiguration mAppBarConfiguration;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private NavController navController;
    private int menuClick = -1;

    private ImageView img_upload;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IFCMService ifcmService;
    private Uri imgUri = null;

    private FirebaseStorage storage;
    private StorageReference storageReference;

    private AlertDialog dialog;

    @OnClick(R.id.fab_chat)
    void onOpenChatList() {
        startActivity(new Intent(this, ChatListActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);


        init();

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_category, R.id.nav_food_list, R.id.nav_shipper, R.id.nav_sign_out)
                .setDrawerLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.bringToFront();

        View headerView = navigationView.getHeaderView(0);
        TextView txt_user = headerView.findViewById(R.id.txt_user);
        Common.setSpanString("Hi ", Common.currentServerUser.getName(), txt_user);
        menuClick = R.id.nav_category;//default

        checkIsOpenFromActivity();
    }

    private void init() {
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        subscribeToTopic(Common.createTopicOrder());
        updateToken();

        dialog = new AlertDialog.Builder(this).setCancelable(false)
                .setMessage("Please wait...")
                .create();
    }

    private void checkIsOpenFromActivity() {
        boolean isOpenFromNewOrder = getIntent().getBooleanExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER, false);
        if (isOpenFromNewOrder) {
            navController.popBackStack();
            navController.navigate(R.id.nav_order);
            menuClick = R.id.nav_order;

        }
    }

    private void updateToken() {
        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnFailureListener(e -> Toast.makeText(HomeActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnSuccessListener(instanceIdResult -> {
                    Common.updateToken(HomeActivity.this, instanceIdResult.getToken(),
                            true, false);

                    Log.d("MYTOKEN", instanceIdResult.getToken());
                });
    }

    private void subscribeToTopic(String topicOrder) {
        FirebaseMessaging.getInstance()
                .subscribeToTopic(topicOrder)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful())
                        Toast.makeText(this, "Failed: " + task.isSuccessful(), Toast.LENGTH_SHORT).show();
                });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().removeAllStickyEvents();
        EventBus.getDefault().unregister(this);
        compositeDisposable.clear();
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCategoryClick(CategoryClick event) {
        if (event.isSuccess()) {
            if (menuClick != R.id.nav_food_list) {
                navController.navigate(R.id.nav_food_list);
                menuClick = R.id.nav_food_list;
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onToastEvent(ToastEvent event) {
        if (event.getAction() == Common.ACTION.CREATE) {
            Toast.makeText(this, "Create Successful", Toast.LENGTH_SHORT).show();
        }
        else if (event.getAction() == Common.ACTION.UPDATE) {
            Toast.makeText(this, "Update Successful", Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this, "Delete Successful", Toast.LENGTH_SHORT).show();
        }
        EventBus.getDefault().postSticky(new ChangeMenuClick(event.isFromFoodList()));
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onChangeMenuClick(ChangeMenuClick event) {
        if (event.isFromFoodList()) {
            //clear
            navController.popBackStack(R.id.nav_category, true);
            navController.navigate(R.id.nav_category);
        } else {
            //clear
            navController.popBackStack(R.id.nav_food_list, true);
            navController.navigate(R.id.nav_food_list);
        }
        menuClick = -1;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        menuItem.setChecked(true);
        drawer.closeDrawers();
        switch (menuItem.getItemId()) {
            case R.id.nav_category:
                if (menuItem.getItemId() != menuClick) {
                    navController.popBackStack();//remove all back stack
                    navController.navigate(R.id.nav_category);
                }
                break;
            case R.id.nav_order:
                if (menuItem.getItemId() != menuClick) {
                    navController.popBackStack();//remove all back stack
                    navController.navigate(R.id.nav_order);
                }
                break;
            case R.id.nav_shipper:
                if (menuItem.getItemId() != menuClick) {
                    navController.popBackStack();//remove all back stack
                    navController.navigate(R.id.nav_shipper);
                }
                break;
            case R.id.nav_best_deals:
                if (menuItem.getItemId() != menuClick) {
                    navController.popBackStack();//remove all back stack
                    navController.navigate(R.id.nav_best_deals);
                }
                break;
            case R.id.nav_most_popular:
                if (menuItem.getItemId() != menuClick) {
                    navController.popBackStack();//remove all back stack
                    navController.navigate(R.id.nav_most_popular);
                }
                break;
            case R.id.nav_send_news:
                showNewsDialog();
                break;

            case R.id.nav_sign_out:
                signout();
                break;
            default:
                menuClick = -1;
                break;
        }
        menuClick = menuItem.getItemId();
        return true;
    }

    private void showNewsDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("News System");
        builder.setMessage("Send News Notification To All Clients");
        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_news_system, null);

        //Views
        EditText edt_title = itemView.findViewById(R.id.edt_title);
        EditText edt_content = itemView.findViewById(R.id.edt_content);
        EditText edt_link = itemView.findViewById(R.id.edt_link);
        img_upload = itemView.findViewById(R.id.img_upload);
        RadioButton rdi_none = itemView.findViewById(R.id.rdi_none);
        RadioButton rdi_link = itemView.findViewById(R.id.rdi_link);
        RadioButton rdi_upload = itemView.findViewById(R.id.rdi_image);

        //Event
        rdi_none.setOnClickListener(view -> {
            edt_link.setVisibility(View.GONE);
            img_upload.setVisibility(View.GONE);
        });

        rdi_link.setOnClickListener(view -> {
            edt_link.setVisibility(View.VISIBLE);
            img_upload.setVisibility(View.GONE);
        });

        rdi_upload.setOnClickListener(view -> {
            edt_link.setVisibility(View.GONE);
            img_upload.setVisibility(View.VISIBLE);
        });

        img_upload.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Image From Gallery"), PICK_IMAGE_REQUEST);
        });

        builder.setView(itemView);
        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        builder.setPositiveButton("SEND", (dialogInterface, i) -> {
            if (rdi_none.isChecked()) {
                sendNews(edt_title.getText().toString(), edt_content.getText().toString());
            } else if (rdi_link.isChecked()) {
                sendNewsLink(edt_title.getText().toString(), edt_content.getText().toString(), edt_link.getText().toString());
            } else if (rdi_upload.isChecked()) {
                if (imgUri != null) {
                    AlertDialog dialog = new AlertDialog.Builder(this).setMessage("Uploading...").create();
                    dialog.show();

                    String file_name = UUID.randomUUID().toString();
                    StorageReference newsImages = storageReference.child("news/" + file_name);
                    newsImages.putFile(imgUri)
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            })
                            .addOnSuccessListener(taskSnapshot -> {
                                dialog.dismiss();
                                newsImages.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        sendNewsLink(edt_title.getText().toString(), edt_content.getText().toString(), uri.toString());
                                    }
                                });

                            }).addOnProgressListener(taskSnapshot -> {
                        double progress = Math.round((100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()));
                        dialog.setMessage(new StringBuilder("Uploading: ").append(progress).append("%"));
                    });

                }
            }

        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void sendNewsLink(String title, String content, String url) {
        Map<String, String> notificationData = new HashMap<String, String>();
        notificationData.put(Common.NOTI_TITLE, title);
        notificationData.put(Common.NOTI_CONTENT, content);
        notificationData.put(Common.IS_SEND_IMAGE, "true");
        notificationData.put(Common.IMAGE_URL, url);

        FCMSendData fcmSendData = new FCMSendData(Common.getNewsTopic(), notificationData);

        AlertDialog dialog = new AlertDialog.Builder(this).setMessage("Sending...").create();
        dialog.show();

        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fcmResponse -> {
                    dialog.dismiss();
                    if (fcmResponse.getMessage_id() != 0)
                        Toast.makeText(this, "News Sent Successfully", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(this, "News Failed To Send", Toast.LENGTH_SHORT).show();

                }, throwable -> {
                    dialog.dismiss();
                    Toast.makeText(this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();

                }));
    }

    private void sendNews(String title, String content) {
        Map<String, String> notificationData = new HashMap<String, String>();
        notificationData.put(Common.NOTI_TITLE, title);
        notificationData.put(Common.NOTI_CONTENT, content);
        notificationData.put(Common.IS_SEND_IMAGE, "false");

        FCMSendData fcmSendData = new FCMSendData(Common.getNewsTopic(), notificationData);

        AlertDialog dialog = new AlertDialog.Builder(this).setMessage("Sending...").create();
        dialog.show();

        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fcmResponse -> {
                    dialog.dismiss();
                    if (fcmResponse.getMessage_id() != 0)
                        Toast.makeText(this, "News Sent Successfully", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(this, "News Failed To Send", Toast.LENGTH_SHORT).show();

                }, throwable -> {
                    dialog.dismiss();
                    Toast.makeText(this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();

                }));

    }

    private void signout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout")
                .setMessage("Are you sure you want to Logout?")
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("CONFIRM", (dialog, which) -> {
                    Common.selectedFood = null;
                    Common.categorySelected = null;
                    Common.currentServerUser = null;
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                imgUri = data.getData();
                img_upload.setImageURI(imgUri);

            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onPrintEventListener(PrintOrderEvent event) {
        createPDFFile(event.getPath(), event.getOrderModel());

    }

    private void createPDFFile(String path, OrderModel orderModel) {
        dialog.show();

        if (new File(path).exists())
            new File(path).delete();
        try {
            Document document = new Document();
            //save
            PdfWriter.getInstance(document, new FileOutputStream(path));
            //open
            document.open();
            //settings
            document.setPageSize(PageSize.A4);
            document.addCreationDate();
            document.addAuthor("Beer Run");
            document.addCreator(Common.currentServerUser.getName());

            //Font Setting
            BaseColor colorAccent = new BaseColor(0, 153, 204, 255);
            float fontSize = 20.0f;
            //custom font
            BaseFont fontName = BaseFont.createFont("assets/fonts/brandon_medium.otf", "UTF-8", BaseFont.EMBEDDED);

            //create title
            Font titleFont = new Font(fontName, 36.0f, Font.NORMAL, BaseColor.BLACK);
            PDFUtils.addNewItem(document, "Order Details", Element.ALIGN_CENTER, titleFont);

            //add more
            Font orderNumberFont = new Font(fontName, fontSize, Font.NORMAL, colorAccent);
            PDFUtils.addNewItem(document, "Order No: ", Element.ALIGN_LEFT, orderNumberFont);

            Font orderNumberValueFont = new Font(fontName, fontSize, Font.NORMAL, colorAccent);
            PDFUtils.addNewItem(document, orderModel.getKey(), Element.ALIGN_LEFT, orderNumberValueFont);

            PDFUtils.addLineSeparator(document);

            //date
            PDFUtils.addNewItem(document, "Order Date: ", Element.ALIGN_LEFT, orderNumberFont);
            PDFUtils.addNewItem(document, new SimpleDateFormat("dd/MM/yyyy").format(orderModel.getCreateDate()), Element.ALIGN_LEFT, orderNumberValueFont);

            PDFUtils.addLineSeparator(document);

            //Account name
            PDFUtils.addNewItem(document, "Client Name: ", Element.ALIGN_LEFT, orderNumberFont);
            PDFUtils.addNewItem(document, (orderModel.getUserName()), Element.ALIGN_LEFT, orderNumberValueFont);

            PDFUtils.addLineSeparator(document);

            //add product and details
            PDFUtils.addLineSpace(document);
            PDFUtils.addNewItem(document, "Product Detail: ", Element.ALIGN_CENTER, titleFont);
            PDFUtils.addLineSeparator(document);

            //use rxjava fetch image from internet and add to pdf
            Observable.fromIterable(orderModel.getCartItemList())
                    .flatMap(cartItem -> Common.getBitmapFromUrl(HomeActivity.this, cartItem, document))
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            cartItem -> {
                                //on next
                                //food/alcohol name
                                PDFUtils.addNewItemWithLeftAndRight(document, cartItem.getFoodName(),
                                        ("(0.0%)"),
                                        titleFont,
                                        orderNumberValueFont);
                                //size and addon
                                PDFUtils.addNewItemWithLeftAndRight(document,
                                        "Size: ",
                                        Common.formatSizeJsonToString(cartItem.getFoodSize())
                                        , titleFont, orderNumberValueFont);

                                PDFUtils.addNewItemWithLeftAndRight(document,
                                        "Addon: ",
                                        Common.formatAddonJsonToString(cartItem.getFoodAddon())
                                        , titleFont, orderNumberValueFont);

                                //price
                                PDFUtils.addNewItemWithLeftAndRight(document,
                                        new StringBuilder().append(cartItem.getFoodQuantity())
                                                .append("x")
                                                .append(cartItem.getFoodPrice() + cartItem.getFoodExtraPrice())
                                                .toString(),
                                        new StringBuilder()
                                                .append(cartItem.getFoodQuantity() *
                                                        cartItem.getFoodPrice()
                                                        + cartItem.getFoodExtraPrice())
                                                .toString()
                                        , titleFont, orderNumberValueFont);

                                PDFUtils.addLineSeparator(document);


                            }
                            , throwable -> {
                                dialog.dismiss();
                                Toast.makeText(this, "error:" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                            , () -> {
                                //on completed
                                //append total
                                PDFUtils.addLineSpace(document);
                                PDFUtils.addLineSpace(document);

                                PDFUtils.addNewItemWithLeftAndRight(document,
                                        "Total: ",
                                        new StringBuilder()
                                                .append(orderModel.getTotalPayment())
                                                .toString()
                                        , titleFont, titleFont);

                                //close
                                document.close();
                                dialog.dismiss();
                                Toast.makeText(this, "Successful", Toast.LENGTH_SHORT).show();

                                printPDF();
                            }
                    );


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printPDF() {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        try {
            PrintDocumentAdapter printDocumentAdapter = new PdfDocumentAdapter(this,
                    new StringBuilder(Common.getAppPath(this))
                            .append(Common.FILE_PRINT).toString());
            printManager.print("Document",printDocumentAdapter,new PrintAttributes.Builder().build());

        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
