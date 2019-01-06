package com.example.user.lista9;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.model.XYSeries;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Acceleration extends Activity implements SensorEventListener {

    //klasa Acceleration (rozszerzająca Activity i implementująca SensorEventListener)
    // odpowiada za aktywność aplikacji związaną z pomiarami przyspieszenia

    //atrybuty klasy
    private static final String TAG = "MainActivity";

    private SensorManager mySensorManager;
    private Sensor accelerometer;
    private PowerManager powerManager;
    private PowerManager.WakeLock myWakeLock;

    private boolean isRunning = false;

    private EditText editTxtFileName;
    private TextView textViewAx;
    private TextView textViewAy;
    private TextView textViewAz;

    private Intent i;
    private XYSeries seriesX;
    private XYSeries seriesY;
    private XYSeries seriesZ;
    private StringBuilder data=new StringBuilder();

    private int counter=0;
    private ArrayList<Double> values=new ArrayList<>();
    private double [] toGive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //metoda przy tworzeniu aktywności
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acceleration);

        //serie danych do wykresu
        seriesX=new XYSeries("X");
        seriesY=new XYSeries("Y");
        seriesZ=new XYSeries("Z");

        //utworzenie intencji do aktywnosci plotActivity
        i = new Intent(getBaseContext(), plotActivity.class);

        //elementy gui
        editTxtFileName=(EditText) findViewById(R.id.editTxtFileName);
        textViewAx=(TextView) findViewById(R.id.accelerationTxt);
        textViewAy=(TextView) findViewById(R.id.accelerationTxt2);
        textViewAz=(TextView) findViewById(R.id.accelerationTxt3);

        //ustawienie czujnika - akcelerometru
        mySensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mySensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        //wake lock zeby aplikacja mogla dzialac, gdy telefon zostanie zablokowany
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        myWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myapp:test");


    }

    public void onClickAction(View view) {
        //metoda po kliknieciu start

        Log.d(TAG, "Button pressed");
        isRunning = !isRunning; //zmienna ktora docyduje o tym, czy dokonujemy pomiaru czy tez nie

        if (isRunning) {
            myWakeLock.acquire(); //jesli pomiar ma byc wykonany musimy tez pozwolic aplikacji na pomiary przy zablokowanym telefonie
        }else {
            myWakeLock.release(); //gdy zatrzymujemy pomiar wylaczamy tę funkcję

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //metoda reagujaca na zmianę wartosci rejestrowanej przez czujnik

        if (isRunning) { //gdy pomiar jest wykonywany

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) { //gdy tym sensora to akcelerometr

                //zapisujemy do zmiennych skladowe mierzonego przyspieszenia oraz czas
                float aX = event.values[0];
                float aY=event.values[1];
                float aZ=event.values[2];
                float timeStamp=event.timestamp;

                counter++;// zwiekszam licznik
                //licznik jest potrzebny do wykonywania wykresu
                // (kolejne wartosci są bardziej czytelne niż timeStamp w nanosekundach)

                //wyswietlam wartosci skladowej x przysieszenia i czasu
                Log.d(TAG, "aX= " + Float.toString(aX) +" timeStamp "+Float.toString(timeStamp));

                //wyświetlanie w okienkach aktualnych wartosci skladowych przyspieszenia (sformatowanych)

                String x=String.format("%.2f", Float.toString(aX));
                String y=String.format("%.2f", Float.toString(aY));
                String z=String.format("%.2f", Float.toString(aZ));

                textViewAx.setText(x);
                textViewAy.setText(y);
                textViewAz.setText(z);

//                if(aX<0) textViewAx.setText(Float.toString(aX).subSequence(0,6));
//                else textViewAx.setText(Float.toString(aX).subSequence(0,5));
//
//                if(aY<0) textViewAy.setText(Float.toString(aY).subSequence(0,6));
//                else textViewAy.setText(Float.toString(aY).subSequence(0,5));
//
//                if(aZ<0) textViewAz.setText(Float.toString(aZ).subSequence(0,6));
//                else textViewAz.setText(Float.toString(aZ).subSequence(0,5));

                //dodanie do serii danych wartosci skladowych przyspieszenia
                seriesX.add(counter, aX);
                seriesY.add(counter, aY);
                seriesZ.add(counter, aZ);
                //dodanie wartosci skladowej przyspieszenia do listy tablicowej
                values.add((double) aZ);

                //zapisanie w zmiennej typu StringBuilder wartosci, zeby mozna bylo je zapisac do pliku
                String toData=counter+";"+aX+";"+aY+";"+aZ+"!";
                data.append(toData);

            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //nieuzywana metoda ktora trzeba bylo zaimplementować z interfejsu
    }

    public void plotActiivty(View view) {
        //klikniecie przycisku, ktory włącza nowe okno, w ktorym rysujemy wykres

        isRunning=false; //zatrzymanie pomiaru

        //w nowej intencji moge podać tablice typu double a nie listę tablicową, więc przepisuje wartości
        toGive=new double[values.size()];
        for (int i=0; i<toGive.length; i++){
            toGive[i]=values.get(i);
        }
        //podaje dane do nowej aktywnosci
        i.putExtra("dataX", seriesX);
        i.putExtra("dataY", seriesY);
        i.putExtra("dataZ", seriesZ);
        i.putExtra("steps", toGive);
        startActivity(i); //rozpoczęcie nowej aktywnosci

    }

    public void saveData(View view) {
        //metoda po kliknięciu przycisku zapisania danych do pliku

        //wylaczanie pomiaru
        isRunning=false;

        String name;
        //jesli nie ma nazwy pliku wpisanej przez uzytkownika ustawiam domyslna
        if(editTxtFileName.getText().toString().equals("")||editTxtFileName.getText().toString()==null){
            name="data";
        }else{ //jesli jest nazwa wpisana przez uzytkownika to ją pobieram
            name=editTxtFileName.getText().toString();
        }
        name=name+".txt"; //dodaje rozszerzenie do nazwy
        //wywołuje metodę ktora zapisuje dane do pliku
        saveData(name,data.toString());

    }

    public void clearData(View view) {
        //metoda wywoływana po kliknieciu przycisku czyszczącego dane
        //usuwanie danych z serii danych
        seriesX.clear();
        seriesY.clear();
        seriesZ.clear();
        //zerowanie licznika
        counter=0;
        //zerowanie danych wpisywanych do pliku
        data=new StringBuilder();
        //usuwanie wyswietlonych w okienkach wartosci
        textViewAx.setText("");
        textViewAy.setText("");
        textViewAz.setText("");
        editTxtFileName.setText("");

    }

    private void saveData( String name, String text){

        //metoda slużąca do zapisywania do pliku
        //parametrami metody są nazwa pliku i tekst do zapisania

        try {
            //utworzenie pliku do zapisu
            FileOutputStream fOut = openFileOutput(name,
                    MODE_PRIVATE);
            //utworzenie OutputStreamWritera
            OutputStreamWriter out = new OutputStreamWriter(fOut);
            //zapisanie do pliku
            out.write(text);
            out.flush();
            out.close();

            //wyswietlenie komunikatu, że zapisano dane
            Toast.makeText(this,"Data Saved",Toast.LENGTH_LONG).show();

        } catch (java.io.IOException e) {
            //obsluga wyjatku
            //w razie niepowodzenia zapisu do pliku zostaje wyswietlony komunikat a w konsoli zrzut stosu
            Toast.makeText(this, "Data Could not be added",Toast.LENGTH_LONG).show();
            e.printStackTrace();

        }

    }

}
