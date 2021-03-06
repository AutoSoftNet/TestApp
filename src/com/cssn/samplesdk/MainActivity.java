/*
 *
 */
package com.cssn.samplesdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.cssn.mobilesdk.*;
import com.cssn.mobilesdk.exceptions.AuthorizationException;
import com.cssn.mobilesdk.exceptions.ConnectionException;
import com.cssn.mobilesdk.util.Utils;
import com.cssn.mobilesdk.utilities.CSSNUtil;
import com.cssn.samplesdk.model.MainActivityModel;
import com.cssn.samplesdk.model.MainActivityModel.State;
import com.cssn.samplesdk.util.DataContext;
import com.cssn.samplesdk.util.Util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 */
public class MainActivity extends Activity implements WebServiceListener, CardCroppingListener {

    private static final String TAG = MainActivity.class.getName();
    private static final String IS_SHOWING_DIALOG_KEY = "isShowingDialog";
    private static final String IS_SHOWDUPLEXDIALOG_DIALOG_KEY = "isShowDuplexDialog";
    private static final String IS_PROCESSING_DIALOG_KEY = "isProcessing";
    private static final String IS_CROPPING_DIALOG_KEY = "isCropping";
    private static final String IS_VALIDATING_DIALOG_KEY = "isValidating";
    private static final String IS_ACTIVATING_DIALOG_KEY = "isActivating";
    private static String sPdf417String = "";
    CSSNMobileSDKController cssnMobileSdkControllerInstance = null;
    private ImageView frontImageView;
    private ImageView backImageView;
    private TextView txtTapToCaptureFront;
    private TextView txtTapToCaptureBack;
    private Button processCardButton;
    private RelativeLayout layoutFrontImage;
    private RelativeLayout layoutBackImage;
    private LinearLayout layoutCards;
    private EditText editTextLicense;
    private MainActivityModel mainActivityModel = null;
    private Button activateLicenseButton;
    private static ProgressDialog progressDialog;
    private static AlertDialog showDuplexAlertDialog;
    private static AlertDialog alertDialog;
    private static boolean isShowErrorAlertDialog;
    private static boolean isProcessing;
    private static boolean isShowDuplexDialog;
    private static boolean isValidating;
    private static boolean isActivating;
    private static boolean isCropping;
    private MainActivity mainActivity;

    /**
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Util.LOG_ENABLED) {
            Log.v(TAG, "protected void onCreate(Bundle savedInstanceState)");
        }

        // load the model
        if (savedInstanceState == null) {
            if (Util.LOG_ENABLED) {
                Log.v(TAG, "if (savedInstanceState == null)");
            }
            mainActivityModel = new MainActivityModel();
        } else {
            if (Util.LOG_ENABLED) {
                Log.v(TAG, "if (savedInstanceState != null)");
            }
            mainActivityModel = DataContext.getInstance().getMainActivityModel();
            // if coming from background and kill the app, restart the model
            if (mainActivityModel == null) {
                mainActivityModel = new MainActivityModel();
            }
        }
        DataContext.getInstance().setContext(getApplicationContext());

        // load the controller instance
        cssnMobileSdkControllerInstance = CSSNMobileSDKController.getInstance(this);

        if (!Util.isTablet(this)) {
            cssnMobileSdkControllerInstance.setPdf417BarcodeImageDrawable(getResources().getDrawable(R.drawable.barcode));
        }

        cssnMobileSdkControllerInstance.setWebServiceListener(this);
        cssnMobileSdkControllerInstance.setCloudUrl("cssnwebservices.com");
        cssnMobileSdkControllerInstance.setWatermarkText("Powered By Acuant", 0, 30, 0, 0);

        // load several member variables
        setContentView(R.layout.activity_main);

        layoutCards = (LinearLayout) findViewById(R.id.cardImagesLayout);
        layoutBackImage = (RelativeLayout) findViewById(R.id.relativeLayoutBackImage);
        layoutFrontImage = (RelativeLayout) findViewById(R.id.relativeLayoutFrontImage);

        frontImageView = (ImageView) findViewById(R.id.frontImageView);
        backImageView = (ImageView) findViewById(R.id.backImageView);

        editTextLicense = (EditText) findViewById(R.id.editTextLicenceKey);
        editTextLicense.setText(DataContext.getInstance().getLicenseKey());

        txtTapToCaptureFront = (TextView) findViewById(R.id.txtTapToCaptureFront);
        txtTapToCaptureBack = (TextView) findViewById(R.id.txtTapToCaptureBack);

        activateLicenseButton = (Button) findViewById(R.id.activateLicenseButton);

        processCardButton = (Button) findViewById(R.id.processCardButton);
        processCardButton.setVisibility(View.INVISIBLE);

        editTextLicense.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                validateLicenseKey(editTextLicense.getText().toString());
                DataContext.getInstance().setLicenseKey(editTextLicense.getText().toString());
                return true;
            }
        });

        // it is necessary to use a post UI call, because of the previous set text on 'editTextLicense'
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                editTextLicense.addTextChangedListener(new TextWatcher() {
                    public void afterTextChanged(Editable s) {
                        mainActivityModel.setState(State.NO_VALIDATED);
                        updateActivateLicenseButtonFromModel();
                    }

                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }
                });
            }
        });

        editTextLicense.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideVirtualKeyboard();
                }
            }
        });

        // update the UI from the model
        updateUI();
        cssnMobileSdkControllerInstance.setCardCroppingListener(this);

        // validate the license key
        if (mainActivityModel.getState() == null && !isValidating) // only once
        {
            validateLicenseKey(editTextLicense.getText().toString());
        }

        if (Utils.LOG_ENABLED) {
            Log.v(TAG, "getScreenOrientation()=" + Util.getScreenOrientation(this));
        }
    }


    /**
     *
     */
    private void validateLicenseKey(String licenseKey) {
        Util.lockScreen(MainActivity.this);
        DataContext.getInstance().setLicenseKey(editTextLicense.getText().toString());
        progressDialog = Util.showProgessDialog(this, "Validating License ..");
        isValidating = true;
        cssnMobileSdkControllerInstance.setLicensekey(licenseKey);
        hideVirtualKeyboard();
    }

    /**
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (Util.LOG_ENABLED) {
            Log.v(TAG, "protected void onActivityResult(int requestCode, int resultCode, Intent data)");
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        updateUI();
    }

    @Override
    public void onCardCroppingStart(Activity activity) {
        if (Utils.LOG_ENABLED) {
            Log.v(TAG, "public void onCardCroppingStart(Activity activity)");
        }
        Util.dismissDialog(progressDialog);
        Util.lockScreen(this);
        progressDialog = Util.showProgessDialog(activity, "Cropping image...");
        isCropping = true;
    }

    /**
     * Result from the CSSNMobileSDKController.showCameraInterface method when
     * popover == true
     */
    @Override
    public void onCardCroppingFinish(final Bitmap bitmap) {
        if (Util.LOG_ENABLED) {
            Log.v("appendLog", "public void onCardCroppedFinish(final Bitmap bitmap) - begin");
        }

        Util.unLockScreen(this);

        if (mainActivityModel.getCurrentOptionType() == CardType.DL_DUPLEX) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run(){
                    showDuplexDialog();
                }
            }, 1000);
        }
        updateModelAndUIFromCroppedCard(bitmap);

        if (Util.LOG_ENABLED) {
            Log.v("appendLog", "public void onCardCroppedFinish(final Bitmap bitmap) - end");
        }
        isCropping = false;
    }

    private void showDuplexDialog() {
        mainActivity = this;
        Util.dismissDialog(showDuplexAlertDialog);
        Util.dismissDialog(alertDialog);
        showDuplexAlertDialog = new AlertDialog.Builder(this).create();
        showDuplexAlertDialog = Util.showDialog(this, getString(R.string.dl_duplex_dialog),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    cssnMobileSdkControllerInstance.showCameraInterface(mainActivity, CardType.DL_DUPLEX, false);
                } catch (AuthorizationException e) {
                    e.printStackTrace();
                } catch (ConnectionException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
                isShowDuplexDialog = false;
            }
        } , new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                mainActivityModel.setCurrentOptionType(CardType.DL_DUPLEX);
                highlightCurrentCardOption();
                dialog.dismiss();
                isShowDuplexDialog = false;
            }
        });
        isShowDuplexDialog = true;
    }

    @Override
    public void onPDF417Finish(String result) {

        if (result == null) {
            resetPdf417String();
            mainActivityModel.setCurrentOptionType(CardType.DRIVERS_LICENSE);
            highlightCurrentCardOption();
        } else
            sPdf417String = result;

    }

    /**
     * Updates the model, and the ui. Called after acquiring a cropped card.
     */
    private void updateModelAndUIFromCroppedCard(final Bitmap bitmap) {
        switch (mainActivityModel.getCardSideSelected()) {
            case FRONT:
                mainActivityModel.setFrontSideCardImage(bitmap);
                break;

            case BACK:
                mainActivityModel.setBackSideCardImage(bitmap);
                break;

            default:
                throw new IllegalStateException("This method is bad implemented, there is not processing for the cardSide '"
                        + mainActivityModel.getCardSideSelected() + "'");
        }

        if (bitmap == null) {
            // set an error to be shown in the onResume method.
            mainActivityModel.setErrorMessage("Unable to detect the card. Please try again.");
        }

        updateUI();
    }

    /**
     * @param v
     */
    public void frontSideCapturePressed(View v) {
        if (Util.LOG_ENABLED) {
            Log.v(TAG, "public void frontSideCapturePressed(View v)");
        }

        mainActivityModel.clearImages();

        mainActivityModel.setCardSideSelected(MainActivityModel.CardSide.FRONT);

        showCameraInterface();
    }

    /**
     * @param v
     */
    public void backSideCapturePressed(View v) {
        if (Util.LOG_ENABLED) {
            Log.v(TAG, "public void backSideCapturePressed(View v)");
        }

        //mainActivityModel.clearImages();

        mainActivityModel.setCardSideSelected(MainActivityModel.CardSide.BACK);
        showCameraInterface();
    }

    /**
     *
     */
    private void showCameraInterface() {
        final int currentOptionType = mainActivityModel.getCurrentOptionType();
        alertDialog = new AlertDialog.Builder(this).create();

        try {
            // XXX this seems to be temporary. It is only used to speed up the cropping.
            if (currentOptionType == CardType.DL_DUPLEX) {
                cssnMobileSdkControllerInstance.setInitialMessageDescriptor(R.layout.hold_steady);
                cssnMobileSdkControllerInstance.setFinalMessageDescriptor(R.layout.align_and_tap);
            } else {
                if (currentOptionType == CardType.PASSPORT) {
                    cssnMobileSdkControllerInstance.setWidth(CSSNUtil.DEFAULT_CROP_PASSPORT_WIDTH);
                    cssnMobileSdkControllerInstance.setHeight(CSSNUtil.DEFAULT_CROP_PASSPORT_HEIGHT);
                    cssnMobileSdkControllerInstance.setInitialMessageDescriptor(R.layout.tap_to_focus);
                } else {
                    cssnMobileSdkControllerInstance.setWidth(CSSNUtil.DEFAULT_CROP_DRIVERS_LICENSE_WIDTH);
                    cssnMobileSdkControllerInstance.setHeight(CSSNUtil.DEFAULT_CROP_DRIVERS_LICENSE_HEIGHT);
                    cssnMobileSdkControllerInstance.setInitialMessageDescriptor(R.layout.hold_steady);
                    cssnMobileSdkControllerInstance.setFinalMessageDescriptor(R.layout.align_and_tap);
                }
            }
            cssnMobileSdkControllerInstance.showCameraInterface(this, currentOptionType, true);

        } catch (AuthorizationException e) {
            Log.e(TAG, e.getMessage(), e);
            Util.dismissDialog(alertDialog);
            alertDialog = Util.showDialog(this, e.getMessage(),new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isShowErrorAlertDialog = false;
                }
            });
            isShowErrorAlertDialog = true;
        } catch (ConnectionException e) {
            Log.e(TAG, e.getMessage(), e);
            Util.dismissDialog(alertDialog);
            alertDialog = Util.showDialog(this, e.getMessage(), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isShowErrorAlertDialog = false;
                }
            });
            isShowErrorAlertDialog = true;
        }
    }

    /**
     * Called after a tap in the driver's card button.
     *
     * @param v
     */
    public void driverCardButtonPressed(View v) {
        // update the model

        mainActivityModel.setCurrentOptionType(CardType.DRIVERS_LICENSE);
        mainActivityModel.clearImages();
        updateUI();
    }

    /**
     * Called after a tap in the driver's card button.
     *
     * @param v
     */
    public void driverCardButtonDuplexPressed(View v) {
        // update the model

        mainActivityModel.setCurrentOptionType(CardType.DL_DUPLEX);
        mainActivityModel.clearImages();
        updateUI();
    }

    /**
     * Called after a tap in the passport card button.
     *
     * @param v
     */
    public void passportCardButtonPressed(View v) {
        mainActivityModel.setCurrentOptionType(CardType.PASSPORT);
        mainActivityModel.clearImages();

        updateUI();
    }

    /**
     * Called after a tap in the medical card button.
     *
     * @param v
     */
    public void medicalCardButtonPressed(View v) {

updateUI();
        mainActivityModel.setCurrentOptionType(CardType.MEDICAL_INSURANCE);
        mainActivityModel.clearImages();

        updateUI();
    }

    /**
     * calculate the width and height of the front side card image and resize them
     */
    private void resizeImageFrames(int cardType) {
        double aspectRatio = CSSNUtil.getAspectRatio(cardType);

        int height = (int) (layoutFrontImage.getLayoutParams().width * aspectRatio);
        int width = layoutFrontImage.getLayoutParams().width;

        layoutFrontImage.getLayoutParams().height = height;
        layoutFrontImage.getLayoutParams().width = width;

        layoutFrontImage.setLayoutParams(layoutFrontImage.getLayoutParams());

        if (cardType == CardType.MEDICAL_INSURANCE) {
            layoutBackImage.getLayoutParams().height = height;
            layoutBackImage.getLayoutParams().width = width;

            layoutBackImage.setLayoutParams(layoutBackImage.getLayoutParams());
        }
    }

    /**
     * Updates the card's frame layout, shows/hides the back side card frame,
     * highlights the selected option, and load the card images in the view.
     */
    public void updateUI() {
        if (Utils.LOG_ENABLED) {
            Log.v(TAG, "private void updateUI()");
        }

        if (mainActivityModel.getErrorMessage() != null) {
            Util.dismissDialog(alertDialog);

            alertDialog = Util.showDialog(this, mainActivityModel.getErrorMessage(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mainActivityModel.setErrorMessage(null);
                    isShowErrorAlertDialog = false;
                }
            });
            isShowErrorAlertDialog = true;
        }

        // change orientation issues
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutCards.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            layoutCards.setOrientation(LinearLayout.VERTICAL);
        }

        // enable/disable the activate button
        updateActivateLicenseButtonFromModel();

        if (mainActivityModel.getCurrentOptionType() == -1) {
            // do not do any extra processing
            return;
        }

        // calculate the width and height of the front side card image and resize them
        resizeImageFrames(mainActivityModel.getCurrentOptionType());

        // show/hide the front and back views in the layout
        switch (mainActivityModel.getCurrentOptionType()) {
            case CardType.DL_DUPLEX:
            case CardType.DRIVERS_LICENSE:
            case CardType.PASSPORT:

                txtTapToCaptureFront.setText(getResources().getString(R.string.tap_to_capture));

                showFrontSideCardImage();
                hideBackSideCardImage();
                break;

            case CardType.MEDICAL_INSURANCE:

                txtTapToCaptureFront.setText(R.string.tap_to_capture_front_side);

                showFrontSideCardImage();
                showBackSideCardImage();
                break;

            default:
                throw new IllegalArgumentException(
                        "This method is wrong implemented, there is not processing for the card type '" + mainActivityModel.getCurrentOptionType() + "'");

        }

        // update card in front image view
        frontImageView.setImageBitmap( Util.getRoundedCornerBitmap(mainActivityModel.getFrontSideCardImage(), this.getApplicationContext()));

        if (mainActivityModel.getFrontSideCardImage() != null) {
            hideFrontImageText();
        } else {
            showFrontImageText();
        }

        // update card in back image view
        backImageView.setImageBitmap(Util.getRoundedCornerBitmap(mainActivityModel.getBackSideCardImage(), this.getApplicationContext()));

        if (mainActivityModel.getBackSideCardImage() != null) {
            hideBackImageText();
        } else {
            showBackImageText();
        }

        // update the process button
        if (mainActivityModel.getFrontSideCardImage() != null) {
            processCardButton.setVisibility(View.VISIBLE);
        } else {
            processCardButton.setVisibility(View.GONE);
        }

        highlightCurrentCardOption();

    }

    /**
     * Highlights the current option: drivers card, medical or passport.
     */
    private void highlightCurrentCardOption() {
        int buttonId;

        switch (mainActivityModel.getCurrentOptionType()) {

            case CardType.DL_DUPLEX:

                buttonId = R.id.buttonDriverDuplex;

                break;

            case CardType.DRIVERS_LICENSE:

                buttonId = R.id.buttonDriver;

                break;

            case CardType.PASSPORT:

                buttonId = R.id.buttonPassport;

                break;

            case CardType.MEDICAL_INSURANCE:

                buttonId = R.id.buttonMedical;

                break;

            default:
                throw new IllegalArgumentException(
                        "This method is wrong implemented, there is not processing for the card type '"
                                + mainActivityModel.getCurrentOptionType() + "'");

        }

        ((Button) findViewById(R.id.buttonDriverDuplex)).setTypeface(null, Typeface.NORMAL);
        ((Button) findViewById(R.id.buttonDriver)).setTypeface(null, Typeface.NORMAL);
        ((Button) findViewById(R.id.buttonPassport)).setTypeface(null, Typeface.NORMAL);
        ((Button) findViewById(R.id.buttonMedical)).setTypeface(null, Typeface.NORMAL);

        ((Button) findViewById(buttonId)).setTypeface(null, Typeface.BOLD);
    }

    /**
     * Called by the process Button
     *
     * @param v
     */
    public void processCard(View v) {
        if (!isProcessing) {
            isProcessing = true;
            // check for the internet connection
            if (!Utils.isNetworkAvailable(this)) {
                String msg = getString(R.string.no_internet_message);
                Log.e(TAG, msg);
                Util.dismissDialog(alertDialog);
                alertDialog = Util.showDialog(this, msg,new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isShowErrorAlertDialog = false;
                    }
                });
                isShowErrorAlertDialog = true;
                return;
            }

            // process the card
            progressDialog = Util.showProgessDialog(this, "Capturing data ...");

            Util.lockScreen(this);

            ProcessImageRequestOptions options = ProcessImageRequestOptions.getInstance();
            options.autoDetectState = true;
            options.stateID = -1;
            options.reformatImage = true;
            options.reformatImageColor = 0;
            options.DPI = 150;
            options.cropImage = false;
            options.faceDetec = true;
            options.signDetec = true;
            options.iRegion = 0;
            options.CSSNCardType = mainActivityModel.getCurrentOptionType();

            cssnMobileSdkControllerInstance.callProcessImageServices(mainActivityModel.getFrontSideCardImage(), mainActivityModel.getBackSideCardImage(), sPdf417String, this, options);

            resetPdf417String();

        }
    }

    private void resetPdf417String() {
        sPdf417String = "";
    }

    /**
     * @param v
     */
    public void activateLicenseKey(View v) {
        hideVirtualKeyboard();

        String key = editTextLicense.getText().toString().trim();
        if (!key.equals("")) {
            Util.lockScreen(MainActivity.this);
            progressDialog = Util.showProgessDialog(this, "Activating License ..");
            isActivating = true;
            cssnMobileSdkControllerInstance.callActivateLicenseKeyService(key);

        } else {
            Util.dismissDialog(alertDialog);
            alertDialog = Util.showDialog(this, "The license key cannot be empty.",new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isShowErrorAlertDialog = false;
                }
            });
            isShowErrorAlertDialog = true;
        }
    }

    /**
     *
     */
    @Override
    public void processImageServiceCompleted(Card card, int status, String errorMessage) {
        if (Util.LOG_ENABLED) {
            Log.v(TAG, "public void processImageServiceCompleted(CSSNCard card, int status, String errorMessage)");
        }
        isProcessing = false;

        Util.dismissDialog(progressDialog);

        String dialogMessage = null;

        try {
            DataContext.getInstance().setCardType(mainActivityModel.getCurrentOptionType());

            if (status == ErrorType.CSSNRequestErrorNoneError) {
                if (card == null || card.isEmpty()) {
                    dialogMessage = "No data found for this license card!";
                } else {

                    switch (mainActivityModel.getCurrentOptionType()) {
                        case CardType.DL_DUPLEX:
                            DataContext.getInstance().setProcessedLicenseCardDuplex((DriversLicenseCardDuplex) card);
                            break;
                        case CardType.DRIVERS_LICENSE:
                            DataContext.getInstance().setProcessedLicenseCard((DriversLicenseCard) card);
                            break;

                        case CardType.MEDICAL_INSURANCE:
                            DataContext.getInstance().setProcessedMedicalCard((MedicalCard) card);
                            break;

                        case CardType.PASSPORT:
                            DataContext.getInstance().setProcessedPassportCard((PassportCard) card);
                            break;

                        default:
                            throw new IllegalStateException("There is not implementation for processing the card type '"
                                    + mainActivityModel.getCurrentOptionType() + "'");
                    }

                    Util.unLockScreen(MainActivity.this);

                    Intent showDataActivityIntent = new Intent(this, ShowDataActivity.class);
                    this.startActivity(showDataActivityIntent);
                }
            } else {
                Log.v(TAG, "processImageServiceCompleted, webService returns an error: " + errorMessage);
                dialogMessage = "" + errorMessage;
            }

        } catch (Exception e) {
            Log.v(TAG, e.getMessage(), e);
            dialogMessage = "Sorry! Internal error has occurred, please contact us!";

        }

        if (dialogMessage != null) {
            Util.dismissDialog(alertDialog);
            alertDialog = Util.showDialog(this, dialogMessage,new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isShowErrorAlertDialog = false;
                }
            });
            isShowErrorAlertDialog = true;
        }
    }

    /**
     *
     */
    @Override
    public void activateLicenseKeyCompleted(LicenseActivationDetails cssnLicenseActivationDetails, final int status, String message) {
        Util.dismissDialog(progressDialog);
        Util.unLockScreen(MainActivity.this);
        isActivating = false;

        String msg = message;

        if (status == ErrorType.CSSNRequestErrorNoneError) {
            if (cssnLicenseActivationDetails != null) {
                msg = cssnLicenseActivationDetails.getIsLicenseKeyActivatedDescscription();
            }

        } else {
            if (Util.LOG_ENABLED) {
                Log.v(TAG, "activateLicenseKeyCompleted, error from server:" + message);
            }

            if (status == ErrorType.CSSNRequestErrorCouldNotReachServer) {
                msg = getString(R.string.no_internet_message);
            }

        }

        Util.lockScreen(this);
        alertDialog =  Util.showDialog(this, msg, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Util.dismissDialog((Dialog) dialog);
                Util.unLockScreen(MainActivity.this);
                // validation if there was not error in the activation, because the
                // validation is done before, every time the license key text
                // changes.
                if (status != ErrorType.CSSNRequestErrorCouldNotReachServer) {
                    validateLicenseKey(editTextLicense.getText().toString());
                }
                isShowErrorAlertDialog = false;
            }
        });
        isShowErrorAlertDialog = true;

    }

    /**
     *
     */
    @Override
    public void validateLicenseKeyCompleted(LicenseDetails details, int status, String message) {

        Util.dismissDialog(progressDialog);
        Util.unLockScreen(MainActivity.this);

        LicenseDetails cssnLicenseDetails = DataContext.getInstance().getCssnLicenseDetails();
        DataContext.getInstance().setCssnLicenseDetails(details);

        // update model
        mainActivityModel.setState(State.VALIDATED);
        if (cssnLicenseDetails != null && cssnLicenseDetails.isLicenseKeyActivated()) {
            mainActivityModel.setValidatedStateActivation(State.ValidatedStateActivation.ACTIVATED);
        } else {
            mainActivityModel.setValidatedStateActivation(State.ValidatedStateActivation.NO_ACTIVATED);
        }
        updateActivateLicenseButtonFromModel();
        // message dialogs
        if (status == ErrorType.CSSNRequestErrorNoneError) // the license key may be no valid, but this status is returned ..
        {
//            Util.showDialog(this, "Successful license key validation");
        } else {
            String msg = message;
            if (status == ErrorType.CSSNRequestErrorCouldNotReachServer) {
                msg = getString(R.string.no_internet_message);
            }
            alertDialog = Util.showDialog(this, msg, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isShowErrorAlertDialog = false;
                }
            });
            isShowErrorAlertDialog = true;
        }
        isValidating = false;
    }

    /**
     */
    private void showFrontSideCardImage() {
        layoutFrontImage.setClickable(true);
        layoutFrontImage.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void hideFrontSideCardImage() {
        layoutFrontImage.setClickable(false);
        layoutFrontImage.setVisibility(View.GONE);
    }

    /**
     *
     */
    private void showBackSideCardImage() {
        layoutBackImage.setClickable(true);
        layoutBackImage.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void hideBackSideCardImage() {
        layoutBackImage.setClickable(false);
        layoutBackImage.setVisibility(View.GONE);
    }

    /**
     *
     */
    private void showFrontImageText() {
        txtTapToCaptureFront.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void hideFrontImageText() {
        txtTapToCaptureFront.setVisibility(View.GONE);
    }

    /**
     *
     */
    private void showBackImageText() {
        txtTapToCaptureBack.setVisibility(View.VISIBLE);
    }

    /**
     *
     */
    private void hideBackImageText() {
        txtTapToCaptureBack.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Util.LOG_ENABLED) {
            Log.v(TAG, "protected void onResume()");
        }
        editTextLicense.clearFocus();
        frontImageView.requestFocus();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //spdf417 = savedInstanceState.get(PDF417_STRING_KEY) != null ? (String) savedInstanceState.get(PDF417_STRING_KEY) : "";
        isShowErrorAlertDialog = savedInstanceState.getBoolean(IS_SHOWING_DIALOG_KEY, false);
        isShowDuplexDialog = savedInstanceState.getBoolean(IS_SHOWDUPLEXDIALOG_DIALOG_KEY, false);
        isProcessing = savedInstanceState.getBoolean(IS_PROCESSING_DIALOG_KEY, false);
        isCropping = savedInstanceState.getBoolean(IS_CROPPING_DIALOG_KEY, false);
        isValidating = savedInstanceState.getBoolean(IS_VALIDATING_DIALOG_KEY, false);
        isActivating = savedInstanceState.getBoolean(IS_ACTIVATING_DIALOG_KEY, false);
        if (isShowDuplexDialog) {
            showDuplexDialog();
        }
        if (isProcessing) {
            progressDialog = Util.showProgessDialog(this, "Capturing data ...");
        }
        if (isCropping){
            progressDialog = Util.showProgessDialog(this, "Cropping image...");
        }
        if (isValidating){
            progressDialog = Util.showProgessDialog(this, "Validating License ..");
        }
        if (isActivating){
            progressDialog = Util.showProgessDialog(this, "Activating License ..");
        }
        if (isShowErrorAlertDialog){
            alertDialog.show();
        }
        updateUI();
    }

    /**
     *
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (Util.LOG_ENABLED) {
            Log.v(TAG, "protected void onSaveInstanceState(Bundle outState)");
        }

        DataContext.getInstance().setMainActivityModel(mainActivityModel);
        //outState.putString(PDF417_STRING_KEY, this.pdf417);
        outState.putBoolean(IS_SHOWING_DIALOG_KEY, isShowErrorAlertDialog);
        outState.putBoolean(IS_PROCESSING_DIALOG_KEY, isProcessing);
        outState.putBoolean(IS_CROPPING_DIALOG_KEY, isCropping);
        outState.putBoolean(IS_ACTIVATING_DIALOG_KEY, isActivating);
        outState.putBoolean(IS_VALIDATING_DIALOG_KEY, isValidating);
        outState.putBoolean(IS_SHOWDUPLEXDIALOG_DIALOG_KEY, isShowDuplexDialog);
    }

    /**
     *
     */
    @Override
    protected void onPause() {
        super.onPause();

        if (Utils.LOG_ENABLED) {
            Log.v(TAG, "protected void onPause()");
        }
    }

    /**
     *
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (Util.LOG_ENABLED) {
            Log.v(TAG, "protected void onDestroy()");
        }
    }

    /**
     * @param bitmap
     * @return
     */
    private boolean saveBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd-mm-yyyy");
            String formattedDate = df.format(c.getTime());

            File file = new File("sdcard/CSSNCardCropped" + formattedDate + ".png");
            FileOutputStream fOutputStream = null;

            try {
                fOutputStream = new FileOutputStream(file);

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOutputStream);

                fOutputStream.flush();
                fOutputStream.close();

                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
            } catch (FileNotFoundException e) {
                if (Util.LOG_ENABLED) {
                    Log.e(TAG, e.getMessage(), e);
                }
                Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show();
                return false;
            } catch (IOException e) {
                if (Util.LOG_ENABLED) {
                    Log.e(TAG, e.getMessage(), e);
                }
                Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     */
    private void hideVirtualKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editTextLicense.getWindowToken(), 0);
    }

    /**
     *
     */
    private void updateActivateLicenseButtonFromModel() {
        activateLicenseButton.setEnabled(
                mainActivityModel.getState() == State.NO_VALIDATED || (mainActivityModel.getState() == State.VALIDATED && mainActivityModel.getValidatedStateActivation() == State.ValidatedStateActivation.NO_ACTIVATED));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }
}