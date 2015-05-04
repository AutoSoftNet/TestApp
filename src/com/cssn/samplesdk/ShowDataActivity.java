/*
 * 
 */
package com.cssn.samplesdk;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cssn.mobilesdk.*;
import com.cssn.mobilesdk.util.Constants;
import com.cssn.samplesdk.util.DataContext;
import com.cssn.samplesdk.util.Util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
//import org.apache.commons.lang.math;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.transform.Result;

/**
 * 
 *
 */
public class ShowDataActivity extends Activity
{
    private static final String TAG = ShowDataActivity.class.getName();
    
    public Boolean isError = false;
    ImageView imgFaceViewer;
    ImageView imgSignatureViewer;
    ImageView frontSideCardImageView;
    ImageView backSideCardImageView;

    TextView textViewCardInfo;

    TextView textViewWSResult;

    public final String MyPrefs = "UserDetails";

    int nFields = 17;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        boolean isTablet = getResources().getBoolean(R.bool.isTablet);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            setContentView(R.layout.activity_show_data_landscape);
        }else {
            setContentView(R.layout.activity_show_data);
        }

        /*if (isTablet == false)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }*/


        frontSideCardImageView = (ImageView) findViewById(R.id.frontSideCardImage);
        backSideCardImageView = (ImageView) findViewById(R.id.backSideCardImage);
        imgFaceViewer = (ImageView) findViewById(R.id.faceImage);
        imgSignatureViewer = (ImageView) findViewById(R.id.signatureImage);
        textViewCardInfo = (TextView) findViewById(R.id.textViewLicenseCardInfo);
        textViewWSResult = (TextView) findViewById(R.id.textViewWSResult);

        loadResult();
    }

    private void loadResult()
    {
        if (DataContext.getInstance().getCardType() == CardType.DRIVERS_LICENSE)
        {
            if (DataContext.getInstance().getProcessedLicenseCard() != null)
            {
                setResultFields();
            } else
            {
                Toast.makeText(ShowDataActivity.this, Constants.ERROR_RESULT, Toast.LENGTH_LONG).show();
            }
        }else if (DataContext.getInstance().getCardType() == CardType.DL_DUPLEX)
        {
            if (DataContext.getInstance().getProcessedLicenseCardDuplex() != null)
            {
                setResultFields();
            } else
            {
                Toast.makeText(ShowDataActivity.this, Constants.ERROR_RESULT, Toast.LENGTH_LONG).show();
            }
        } else if (DataContext.getInstance().getCardType() == CardType.MEDICAL_INSURANCE)
        {
            if (DataContext.getInstance().getProcessedMedicalCard() != null)
            {
                setResultFields();
            } else
            {
                Toast.makeText(ShowDataActivity.this, Constants.ERROR_RESULT, Toast.LENGTH_LONG).show();
            }
        } else if (DataContext.getInstance().getCardType() == CardType.PASSPORT)
        {
            if (DataContext.getInstance().getProcessedPassportCard() != null)
            {
                setResultFields();
            } else
            {
                Toast.makeText(ShowDataActivity.this, Constants.ERROR_RESULT, Toast.LENGTH_LONG).show();
            }
        } else
        {
            Toast.makeText(ShowDataActivity.this, Constants.ERROR_RESULT, Toast.LENGTH_LONG).show();
        }

    }

    private void setResultFields()
    {
        try
        {
            backSideCardImageView.setVisibility(View.INVISIBLE);
            
            switch (DataContext.getInstance().getCardType())
            {
                case CardType.DL_DUPLEX:

                    setResultsForDriversLicenseCardDuplex();
                    break;

                case CardType.DRIVERS_LICENSE:
                    
                    setResultsForDriversLicenseCard();
                    break;

                case CardType.MEDICAL_INSURANCE:
                    
                    setResultsForMedicalCard();
                    break;

                case CardType.PASSPORT:
                    
                    setResultsForPassportCard();
                    break;

                default:
                    Log.e(TAG,"Invalid card type. This method is bad implemented or DataContext.getInstance().getCardType() has an invalid card type.");
                    Toast.makeText(ShowDataActivity.this, Constants.ERROR_RESULT, Toast.LENGTH_LONG).show();
                    break;
                    
            }

        } catch (Exception e)
        {
            Log.e(TAG, e.getMessage(), e);
        }

    }

    /**
     * 
     */
    private void setResultsForPassportCard()
    {
        PassportCard processedPassportCard = DataContext.getInstance().getProcessedPassportCard();
        
        StringBuilder info = new StringBuilder();

        // First Name
        info.append(("First Name").concat(" - "))
                .append(processedPassportCard.getNameFirst()).append("<br/>");
        // Middle Name
        info.append(("Middle Name").concat(" - "))
                .append(processedPassportCard.getNameMiddle()).append("<br/>");
        // Last Name
        info.append(("Last Name").concat(" - "))
                .append(processedPassportCard.getNameLast()).append("<br/>");
        // Passport Number
        info.append(("Passport Number").concat(" - "))
                .append(processedPassportCard.getPassportNumber()).append("<br/>");
        // Personal Number
        info.append(("Personal Number").concat(" - "))
                .append(processedPassportCard.getPersonalNumber()).append("<br/>");
        // Sex
        info.append(("Sex").concat(" - ")).append(processedPassportCard.getSex())
                .append("<br/>");
        // Country Long
        info.append(("Country Long").concat(" - "))
                .append(processedPassportCard.getCountryLong()).append("<br/>");
        // Nationality Long
        info.append(("Nationality Long").concat(" - "))
                .append(processedPassportCard.getNationalityLong()).append("<br/>");
        // DOB Long
        info.append(("DOB Long").concat(" - "))
                .append(processedPassportCard.getDateOfBirth4()).append("<br/>");
        // Issue Date
        info.append(("Issue Date Long").concat(" - "))
                .append(processedPassportCard.getIssueDate4()).append("<br/>");
        // Long Expiration
        info.append(("Expiration Date Long").concat(" - "))
                .append(processedPassportCard.getExpirationDate4()).append("<br/>");
        // Place of Birth
        info.append(("Place of Birth").concat(" - "))
                .append(processedPassportCard.getEnd_POB()).append("<br/>");

        textViewCardInfo.setText(Html.fromHtml(info.toString()));

        frontSideCardImageView.setImageBitmap(Util.getRoundedCornerBitmap(processedPassportCard
                .getReformattedImage(), this.getApplicationContext()));
        imgFaceViewer.setImageBitmap(processedPassportCard.getFaceImage());
        imgSignatureViewer.setImageBitmap(processedPassportCard.getSignImage());
    }



    /**
     * 
     */
    private void setResultsForMedicalCard()
    {
        MedicalCard processedMedicalCard = DataContext.getInstance().getProcessedMedicalCard();
        
        StringBuilder info = new StringBuilder();

        // First Name
        info.append(("First Name").concat(" - "))
                .append(processedMedicalCard.getFirstName()).append("<br/>");
        // Last Name
        info.append(("Last Name").concat(" - "))
                .append(processedMedicalCard.getLastName()).append("<br/>");
        // MemberID
        info.append(("MemberID").concat(" - ")).append(processedMedicalCard.getMemberId())
                .append("<br/>");
        // Group No
        info.append(("Group No.").concat(" - "))
                .append(processedMedicalCard.getGroupNumber()).append("<br/>");
        // Copay ER
        info.append(("Copay ER").concat(" - ")).append(processedMedicalCard.getCopayEr())
                .append("<br/>");
        // Copay OV
        info.append(("Copay OV").concat(" - ")).append(processedMedicalCard.getCopayOv())
                .append("<br/>");
        // Copay SP
        info.append(("Copay SP").concat(" - ")).append(processedMedicalCard.getCopaySp())
                .append("<br/>");
        // Copay UC
        info.append(("Copay UC").concat(" - ")).append(processedMedicalCard.getCopayUc())
                .append("<br/>");
        // Coverage
        info.append(("Coverage").concat(" - ")).append(processedMedicalCard.getCoverage())
                .append("<br/>");
        // Date of Birth
        info.append(("Date of Birth").concat(" - "))
                .append(processedMedicalCard.getDateOfBirth()).append("<br/>");
        // Deductible
        info.append(("Deductible").concat(" - "))
                .append(processedMedicalCard.getDeductible()).append("<br/>");
        // Effective Date
        info.append(("Effective Date").concat(" - "))
                .append(processedMedicalCard.getEffectiveDate()).append("<br/>");
        // Employer
        info.append(("Employer").concat(" - ")).append(processedMedicalCard.getEmployer())
                .append("<br/>");
        // Expire Date
        info.append(("Expire Date").concat(" - "))
                .append(processedMedicalCard.getExpirationDate()).append("<br/>");
        // Group Name
        info.append(("Group Name").concat(" - "))
                .append(processedMedicalCard.getGroupName()).append("<br/>");
        // Issuer Number
        info.append(("Issuer Number").concat(" - "))
                .append(processedMedicalCard.getIssuerNumber()).append("<br/>");
        // Other
        info.append(("Other").concat(" - ")).append(processedMedicalCard.getOther())
                .append("<br/>");
        // Payer ID
        info.append(("Payer ID").concat(" - ")).append(processedMedicalCard.getPayerId())
                .append("<br/>");
        // Plan Admin
        info.append(("Plan Admin").concat(" - "))
                .append(processedMedicalCard.getPlanAdmin()).append("<br/>");
        // Plan Provider
        info.append(("Plan Provider").concat(" - "))
                .append(processedMedicalCard.getPlanProvider()).append("<br/>");
        // Plan Type
        info.append(("Plan Type").concat(" - "))
                .append(processedMedicalCard.getPlanType()).append("<br/>");
        // RX Bin
        info.append(("RX Bin").concat(" - ")).append(processedMedicalCard.getRxBin())
                .append("<br/>");
        // RX Group
        info.append(("RX Group").concat(" - ")).append(processedMedicalCard.getRxGroup())
                .append("<br/>");
        // RX ID
        info.append(("RX ID").concat(" - ")).append(processedMedicalCard.getRxId())
                .append("<br/>");
        // RX PCN
        info.append(("RX PCN").concat(" - ")).append(processedMedicalCard.getRxPcn())
                .append("<br/>");
        // Telephone
        info.append(("Telephone").concat(" - "))
                .append(processedMedicalCard.getPhoneNumber()).append("<br/>");
        // Web
        info.append(("Web").concat(" - ")).append(processedMedicalCard.getWebAddress())
                .append("<br/>");
        // Email
        info.append(("Email").concat(" - ")).append(processedMedicalCard.getEmail())
                .append("<br/>");
        // Address
        info.append(("Address").concat(" - "))
                .append(processedMedicalCard.getFullAddress()).append("<br/>");
        // City
        info.append(("City").concat(" - ")).append(processedMedicalCard.getCity())
                .append("<br/>");
        // Zip
        info.append(("Zip").concat(" - ")).append(processedMedicalCard.getZip())
                .append("<br/>");
        // State
        info.append(("State").concat(" - ")).append(processedMedicalCard.getState())
                .append("<br/>");

        textViewCardInfo.setText(Html.fromHtml(info.toString()));

        frontSideCardImageView.setImageBitmap(Util.getRoundedCornerBitmap(processedMedicalCard
                .getReformattedImage(), this.getApplicationContext()));
        
        if (processedMedicalCard.getReformattedImageTwo() != null)
        {
            backSideCardImageView.setVisibility(View.VISIBLE);
            backSideCardImageView.setImageBitmap(Util.getRoundedCornerBitmap(processedMedicalCard
                    .getReformattedImageTwo(), this.getApplicationContext()));
        }
    }

    /**
     * 
     */
    private void setResultsForDriversLicenseCard()
    {
        DriversLicenseCard processedLicenseCard = DataContext.getInstance().getProcessedLicenseCard();
        
        StringBuilder info = new StringBuilder();
        // name
        info.append(("Name").concat(" - ")).append(processedLicenseCard.getName())
                .append("<br/>");
        // first name
        info.append(("First Name").concat(" - "))
                .append(processedLicenseCard.getNameFirst()).append("<br/>");
        // middle name
        info.append(("Middle Name").concat(" - "))
                .append(processedLicenseCard.getNameMiddle()).append("<br/>");
        // last name
        info.append(("Last Name").concat(" - "))
                .append(processedLicenseCard.getNameLast()).append("<br/>");
        // name suffix
        info.append(("Name Suffix").concat(" - "))
                .append(processedLicenseCard.getNameSuffix()).append("<br/>");
        // license id
        info.append(("ID").concat(" - ")).append(processedLicenseCard.getLicenceID())
                .append("<br/>");
        // license
        info.append(("License").concat(" - ")).append(processedLicenseCard.getLicense())
                .append("<br/>");
        // date of birth long
        info.append(("DOB Long").concat(" - "))
                .append(processedLicenseCard.getDateOfBirth4()).append("<br/>");
        // date of birth short
        info.append(("DOB Short").concat(" - "))
                .append(processedLicenseCard.getDateOfBirth()).append("<br/>");
        // date of birth local
        info.append(("Date Of Birth Local").concat(" - "))
                .append(processedLicenseCard.getDateOfBirthLocal()).append("<br/>");
        // issue date long
        info.append(("Issue Date Long").concat(" - "))
                .append(processedLicenseCard.getIssueDate4()).append("<br/>");
        // issue date short
        info.append(("Issue Date Short").concat(" - "))
                .append(processedLicenseCard.getIssueDate()).append("<br/>");
        // issue date local
        info.append(("Issue Date Local").concat(" - "))
                .append(processedLicenseCard.getIssueDateLocal()).append("<br/>");

        // expiration date long
        info.append(("Expiration Date Long").concat(" - "))
                .append(processedLicenseCard.getExpirationDate4()).append("<br/>");
        // expiration date short
        info.append(("Expiration Date Short").concat(" - "))
                .append(processedLicenseCard.getExpirationDate()).append("<br/>");

        // eye color
        info.append(("EyeColor").concat(" - ")).append(processedLicenseCard.getEyeColor())
                .append("<br/>");
        // hair color
        info.append(("HairColor").concat(" - ")).append(processedLicenseCard.getHair())
                .append("<br/>");
        // height
        info.append(("Height").concat(" - ")).append(processedLicenseCard.getHeight())
                .append("<br/>");
        // weight
        info.append(("Weight").concat(" - ")).append(processedLicenseCard.getWeight())
                .append("<br/>");

        // address
        info.append(("Address").concat(" - ")).append(processedLicenseCard.getAddress())
                .append("<br/>");
        // address 2
        info.append(("Address 2").concat(" - "))
                .append(processedLicenseCard.getAddress2()).append("<br/>");
        // address 3
        info.append(("Address 3").concat(" - "))
                .append(processedLicenseCard.getAddress3()).append("<br/>");
        // address 4
        info.append(("Address 4").concat(" - "))
                .append(processedLicenseCard.getAddress4()).append("<br/>");
        // address 5
        info.append(("Address 5").concat(" - "))
                .append(processedLicenseCard.getAddress5()).append("<br/>");
        // address 6
        info.append(("Address 6").concat(" - "))
                .append(processedLicenseCard.getAddress6()).append("<br/>");

        // city
        info.append(("City").concat(" - ")).append(processedLicenseCard.getCity())
                .append("<br/>");
        // zip
        info.append(("Zip").concat(" - ")).append(processedLicenseCard.getZip())
                .append("<br/>");
        // state
        info.append(("State").concat(" - ")).append(processedLicenseCard.getState())
                .append("<br/>");
        // country
        info.append(("Country").concat(" - ")).append(processedLicenseCard.getCounty())
                .append("<br/>");
        // country short
        info.append(("Country short").concat(" - "))
                .append(processedLicenseCard.getCountryShort()).append("<br/>");
        // country long
        info.append(("Country long").concat(" - "))
                .append(processedLicenseCard.getIdCountry()).append("<br/>");

        // license class
        info.append(("Class").concat(" - "))
                .append(processedLicenseCard.getLicenceClass()).append("<br/>");
        // restriction
        info.append(("Restriction").concat(" - "))
                .append(processedLicenseCard.getRestriction()).append("<br/>");
        // sex
        info.append(("Sex").concat(" - ")).append(processedLicenseCard.getSex())
                .append("<br/>");
        // audit
        info.append(("Audit").concat(" - ")).append(processedLicenseCard.getAudit())
                .append("<br/>");
        // Endorsements
        info.append(("Endorsements").concat(" - "))
                .append(processedLicenseCard.getEndorsements()).append("<br/>");
        // Fee
        info.append(("Fee").concat(" - ")).append(processedLicenseCard.getFee())
                .append("<br/>");
        // CSC
        info.append(("CSC").concat(" - ")).append(processedLicenseCard.getCSC())
                .append("<br/>");
        // SigNum
        info.append(("SigNum").concat(" - ")).append(processedLicenseCard.getSigNum())
                .append("<br/>");
        // Text1
        info.append(("Text1").concat(" - ")).append(processedLicenseCard.getText1())
                .append("<br/>");
        // Text2
        info.append(("Text2").concat(" - ")).append(processedLicenseCard.getText2())
                .append("<br/>");
        // Text3
        info.append(("Text3").concat(" - ")).append(processedLicenseCard.getText3())
                .append("<br/>");
        // Type
        info.append(("Type").concat(" - ")).append(processedLicenseCard.getType())
                .append("<br/>");
        // Doc Type
        info.append(("Doc Type").concat(" - ")).append(processedLicenseCard.getDocType())
                .append("<br/>");
        // Father Name
        info.append(("Father Name").concat(" - "))
                .append(processedLicenseCard.getFatherName()).append("<br/>");
        // Mother Name
        info.append(("Mother Name").concat(" - "))
                .append(processedLicenseCard.getMotherName()).append("<br/>");
        // NameFirst_NonMRZ
        info.append(("NameFirst_NonMRZ").concat(" - "))
                .append(processedLicenseCard.getNameFirst_NonMRZ()).append("<br/>");
        // NameFirst_NonMRZ
        info.append(("NameLast_NonMRZ").concat(" - "))
                .append(processedLicenseCard.getNameLast_NonMRZ()).append("<br/>");
        // NameLast1
        info.append(("NameLast1").concat(" - "))
                .append(processedLicenseCard.getNameLast1()).append("<br/>");
        // NameLast2
        info.append(("NameLast2").concat(" - "))
                .append(processedLicenseCard.getNameLast2()).append("<br/>");
        // NameMiddle_NonMRZ
        info.append(("NameMiddle_NonMRZ").concat(" - "))
                .append(processedLicenseCard.getNameMiddle_NonMRZ()).append("<br/>");
        // NameSuffix_NonMRZ
        info.append(("NameSuffix_NonMRZ").concat(" - "))
                .append(processedLicenseCard.getNameSuffix_NonMRZ()).append("<br/>");
        // Nationality
        info.append(("Nationality").concat(" - "))
                .append(processedLicenseCard.getNationality()).append("<br/>");
        // Original
        info.append(("Original").concat(" - ")).append(processedLicenseCard.getOriginal())
                .append("<br/>");
        // PlaceOfBirth
        info.append(("Place Of Birth").concat(" - "))
                .append(processedLicenseCard.getPlaceOfBirth()).append("<br/>");
        // PlaceOfIssue
        info.append(("Place Of Issue").concat(" - ")).append(processedLicenseCard.getPlaceOfIssue()).append("<br/>");
        // Social Security
        info.append(("Social Security").concat(" - ")).append(processedLicenseCard.getSocialSecurity()).append("<br/>");
        info.append("IsAddressCorrected ".concat(" - ")).append(processedLicenseCard.isAddressCorrected()).append("<br/>");
        info.append("IsAddressVerified ".concat(" - ")).append(processedLicenseCard.isAddressVerified()).append("<br/>");



        textViewCardInfo.setText(Html.fromHtml(info.toString()));

        frontSideCardImageView.setImageBitmap(Util.getRoundedCornerBitmap(processedLicenseCard
                .getReformatImage(), this.getApplicationContext()));
        imgFaceViewer.setImageBitmap(processedLicenseCard.getFaceImage());
        imgSignatureViewer.setImageBitmap(processedLicenseCard.getSignImage());
    }

    /**
     *
     */
    private void setResultsForDriversLicenseCardDuplex()
    {
        DriversLicenseCardDuplex processedLicenseCard = DataContext.getInstance().getProcessedLicenseCardDuplex();



        StringBuilder info = new StringBuilder();
        // first name
        info.append("<DLInfo>");
        info.append("<First Name>").append(processedLicenseCard.getNameFirst()).append("</First Name>");
        // middle name
        info.append("<Middle Name>").append(processedLicenseCard.getNameMiddle()).append("</Middle Name>");
        // last name
        info.append("<Last Name>").append(processedLicenseCard.getNameLast()).append("</Last Name>");
        // name suffix
        info.append("<Name Suffix>").append(processedLicenseCard.getNameSuffix()).append("</Name Suffix>");
        //License
        info.append("<License>").append(processedLicenseCard.getLicense()).append("</License>");
        // date of birth long
        info.append("<DOB>").append(processedLicenseCard.getDateOfBirth4()).append("</DOB>");
        // issue date long
        info.append("<Issue Date>").append(processedLicenseCard.getIssueDate4()).append("</Issue Date>");
        // expiration date long
        info.append("<Expiration Date>").append(processedLicenseCard.getExpirationDate4()).append("</Expiration Date>");
        // eye color
        info.append("<EyeColor>").append(processedLicenseCard.getEyeColor()).append("</EyeColor>");
        // hair color
        info.append("<HairColor>").append(processedLicenseCard.getHair()).append("</HairColor>");
        // height
        info.append("<Height>").append(processedLicenseCard.getHeight()).append("</Height>");
        // weight
        info.append("<Weight>").append(processedLicenseCard.getWeight()).append("</Weight>");
        // address
        info.append("<Address>").append(processedLicenseCard.getAddress()).append("</Address>");
        // city
        info.append("<City>").append(processedLicenseCard.getCity()).append("</City>");
        // zip
        info.append("<Zip>").append(processedLicenseCard.getZip()).append("</Zip>");
        // state
        info.append("<State>").append(processedLicenseCard.getState()).append("</State>");
        // country long
        //info.append(("IdCountry").concat(" - "))
        //        .append(processedLicenseCard.getIdCountry()).append("<br/>");

        // license class
        info.append("<Class>").append(processedLicenseCard.getLicenceClass()).append("</Class>");
        // sex
        info.append("<Sex>").append(processedLicenseCard.getSex()).append("</Sex>");
        info.append("<Is Address Corrected>").append(processedLicenseCard.isAddressCorrected()).append("</Is Address Corrected>");
        info.append("<Is Address Verified>").append(processedLicenseCard.isAddressVerified()).append("</Is Address Verified>");
        info.append("<Is Barcode Read>").append(processedLicenseCard.getIsBarcodeRead()).append("</Is Barcode Read>");
        info.append("<Is ID Verified>").append(processedLicenseCard.getIsIDVerified()).append("</Is ID Verified>");
        info.append("<Is Ocr Read>").append(processedLicenseCard.getIsOcrRead()).append("</Is Ocr Read>");
        info.append("<Document Verification Confidence Rating>").append(processedLicenseCard.getDocumentVerificationConfidenceRating()).append("</Document Verification Confidence Rating>");

        textViewCardInfo.setText(info.toString());
//testing change
        frontSideCardImageView.setImageBitmap(Util.getRoundedCornerBitmap(processedLicenseCard
                .getReformatImage(), this.getApplicationContext()));
        imgFaceViewer.setImageBitmap(processedLicenseCard.getFaceImage());
        imgSignatureViewer.setImageBitmap(processedLicenseCard.getSignImage());

        try {
            Bitmap bmp = processedLicenseCard.getReformatImage();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);
            byte[] byteArrayDLPic = stream.toByteArray();
            bmp = processedLicenseCard.getFaceImage();
            stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);
            byte[] byteArrayFacePic = stream.toByteArray();
            bmp = processedLicenseCard.getSignImage();
            stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, stream);
            byte[] byteArraySignaturePic = stream.toByteArray();
            String XMLData = info.toString();
            callSendPicture("123", XMLData, byteArrayDLPic, byteArrayFacePic, byteArraySignaturePic);

        } catch (Exception exc) {
            exc.printStackTrace();
        }


    }

    private void callSendPicture(String CRMID, String XMLData, byte[] DLPic, byte[] FacePic, byte[] SignPic) {
        try {
            MyTask task = new MyTask(CRMID, XMLData, DLPic, FacePic, SignPic);
            task.execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    private class MyTask extends AsyncTask<Void, Void, String> {
        String strParam;
        byte[] byteArrayDLPic;
        byte[] byteArrayFacePic;
        byte[] byteArraySignPic;
        String XMLData;

        public MyTask (String param1, String paramXML, byte[] param2, byte[] param3, byte[] param4) {
            strParam = param1;
            byteArrayDLPic = param2;
            byteArrayFacePic = param3;
            byteArraySignPic = param4;
            XMLData = paramXML;
        }


        @Override
        protected void onPostExecute(String result) {
            try {
                textViewWSResult.setText("returned: " + result.toString());

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }

        @Override
        protected String doInBackground(Void... params) {
            // TODO Auto-generated method stub

            return SendPicture(strParam, XMLData, byteArrayDLPic, byteArrayFacePic, byteArraySignPic);
        }

    }
    private String SendPicture(String CRMID, String XMLData, byte[] DLPic, byte[] FacePic, byte[] SignPic) {
        JSONObject param = new JSONObject();
        try {
            //CRMID As String, DL As String, DLPic As String, DLSignature As String, XMLData As String
            param.put("CRMID", CRMID);
            param.put("DL", Base64.encodeToString(DLPic, Base64.DEFAULT));
            param.put("DLPic", Base64.encodeToString(FacePic, Base64.DEFAULT));
            param.put("DLSignature", Base64.encodeToString(SignPic, Base64.DEFAULT));
            param.put("XMLData", XMLData);
        } catch (JSONException e2) {
            e2.printStackTrace();
        } catch (Exception exc) {
            exc.printStackTrace();
        }



        JSONObject result = null;
        try {
            //result = sendJsonRequest("www.AutosoftAutos.com", 80, "http://www.AutosoftAutos.com/ASN.ashx/greetings2",param);
            //result = sendJsonRequest("www.AutosoftFinance.com", 443, "https://www.AutosoftFinance.com/OLService/Apps.asmx/GetStartupInfo",param);
            result = sendJsonRequest(443, "https://www.autosoftfinance.com/olservice/Apps.asmx/UploadDL",param);
            //result = sendJsonRequest("localhost", 82, "http://localhost:82/ASN.ashx/greetings",param);

        } catch (ClientProtocolException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (JSONException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        if (result != null) {
            try {
                JSONArray A = new JSONArray(result.getString("d"));
                SharedPreferences mySP = getSharedPreferences(MyPrefs,MODE_PRIVATE);
                SharedPreferences.Editor edit = mySP.edit();
                //edit.clear();
                for (int i=0;i<=A.length();i++) {
                    JSONObject user = new JSONObject(A.getString(i));
                    if (user.getString("Name").equals("Success")) {
                        //if value is numeric, that is the CRMID... if not, it is an error
                        return user.getString("Value");
                    }
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return "";
    }
    private JSONObject sendJsonRequest(  int port, String uri, JSONObject param)
            throws ClientProtocolException, IOException, JSONException
    {
        //HttpClient httpClient = new DefaultHttpClient();
        DefaultHttpClient client = new DefaultHttpClient();
        X509HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
        SchemeRegistry registry = new SchemeRegistry();

        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
        registry.register(new Scheme("https", socketFactory, 443));
        SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);
        DefaultHttpClient httpClient = new DefaultHttpClient(mgr, client.getParams());

        // Set verifier
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);



        HttpPost httpPost = new HttpPost(uri);
        httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
        httpPost.addHeader("dataType","json");

        if (param != null)
        {
            HttpEntity bodyEntity = new StringEntity(param.toString(), "utf8");
            httpPost.setEntity(bodyEntity);
        }

        try {
            HttpResponse response = httpClient.execute( httpPost);
            HttpEntity entity = response.getEntity();

            String result = null;
            if (entity != null) {
                InputStream instream = entity.getContent();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(instream));
                StringBuilder sb = new StringBuilder();


                String line = null;
                while ((line = reader.readLine()) != null)
                    sb.append(line + "\n");


                result = sb.toString();
                instream.close();
            }


            httpPost.abort();
            return result != null ? new JSONObject(result) : null;
        } catch (Exception e1) {
            e1.printStackTrace();
            return null;
        }







    }
}
