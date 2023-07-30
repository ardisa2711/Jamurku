package com.ardi.jamurku;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    // Deklarasi elemen tampilan dan variabel-variabel lainnya
    TextView dataSuhu;
    TextView dataKelembaban;
    String previousSuhuValue = "";
    String previousKelembabanValue = "";
    ToggleButton toggle1, toggle2, toggle3, toggle4;
    int dataKipas = 0, dataLampu = 0, dataPompa = 0;

    private static final int NOTIFICATION_ID_SUHU_PANAS = 1;
    private static final int NOTIFICATION_ID_SUHU_DINGIN = 2;
    private static final int NOTIFICATION_ID_KELEMBABAN_LEMBAB = 3;
    private static final int NOTIFICATION_ID_KELEMBABAN_KERING = 4;

    // Gunakan enum untuk status kontrol
    private enum KontrolStatus {
        ON,
        OFF
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inisialisasi elemen tampilan dan referensi Firebase
        dataSuhu = findViewById(R.id.viewsuhu);
        dataKelembaban = findViewById(R.id.viewkelembaban);
        toggle1 = findViewById(R.id.toggleButton);
        toggle2 = findViewById(R.id.toggleButton2);
        toggle3 = findViewById(R.id.toggleButton3);
        toggle4 = findViewById(R.id.toggleButton4);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRefSuhu = database.getReference("Data_Realtime").child("suhu");
        DatabaseReference myRefKelembaban = database.getReference("Data_Realtime").child("kelembaban");

        DatabaseReference myToggle1 = database.getReference("Kontrol").child("kipas");
        DatabaseReference myToggle2 = database.getReference("Kontrol").child("lampu");
        DatabaseReference myToggle3 = database.getReference("Kontrol").child("pompa");
        DatabaseReference myToggle4 = database.getReference("Kontrol").child("otomatis");

        // Mendapatkan data suhu dan kelembaban dari Firebase dan mengatur tampilan sesuai data yang diterima
        myRefSuhu.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Mengambil nilai suhu sebagai Double dari Firebase
                Double suhuDouble = snapshot.getValue(Double.class);

                if (suhuDouble != null) {
                    // Mengubah nilai suhu dari Double menjadi float
                    float suhu = suhuDouble.floatValue();

                    // Menampilkan data suhu dalam bentuk float dengan 2 angka di belakang koma
                    dataSuhu.setText(String.format("%.2f °C", suhu));

                    // Tampilkan notifikasi jika suhu lebih besar dari 29
                    if (suhu > 29) {
                        showNotification("Rumah jamur terlalu panas, nyalakan kipas!", NOTIFICATION_ID_SUHU_PANAS);
                    } else if (suhu < 26) {
                        showNotification("Rumah jamur terlalu dingin, nyalakan lampu pemanas!", NOTIFICATION_ID_SUHU_DINGIN);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Tangani jika terjadi kesalahan saat membaca dari database
            }
        });

        myRefKelembaban.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Mengambil nilai kelembaban sebagai Double dari Firebase
                Double kelembabanDouble = snapshot.getValue(Double.class);

                if (kelembabanDouble != null) {
                    // Mengubah nilai kelembaban dari Double menjadi float
                    float kelembaban = kelembabanDouble.floatValue();

                    // Menampilkan data kelembaban dalam bentuk float dengan 2 angka di belakang koma
                    dataKelembaban.setText(String.format("%.2f %%", kelembaban));

                    // Tampilkan notifikasi jika kelembaban lebih besar dari 90 atau kurang dari 70
                    if (kelembaban > 90) {
                        showNotification("Rumah jamur terlalu lembab, nyalakan lampu pemanas!", NOTIFICATION_ID_KELEMBABAN_LEMBAB);
                    } else if (kelembaban < 70) {
                        showNotification("Rumah jamur terlalu kering, nyalakan pengkabutan!", NOTIFICATION_ID_KELEMBABAN_KERING);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Tangani jika terjadi kesalahan saat membaca dari database
            }
        });


        // Mengatur status toggle dan mendengarkan perubahan status dari Firebase
        setToggleStatus(myToggle1, toggle1, KontrolStatus.OFF);
        setToggleStatus(myToggle2, toggle2, KontrolStatus.OFF);
        setToggleStatus(myToggle3, toggle3, KontrolStatus.OFF);

        // Listener untuk toggle4 (Otomatis)
        toggle4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Mengatur nilai di Firebase berdasarkan status toggle4
                myToggle4.setValue(isChecked ? 1 : 0);
            }
        });

        // Listener untuk mengatur enable/disable toggle1, toggle2, dan toggle3 berdasarkan nilai toggle4
        myToggle4.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer toggle4Value = snapshot.getValue(Integer.class);
                if (toggle4Value != null) {
                    // Matikan atau aktifkan toggle1, toggle2, dan toggle3 berdasarkan nilai myToggle4
                    toggle1.setEnabled(toggle4Value == 0);
                    toggle2.setEnabled(toggle4Value == 0);
                    toggle3.setEnabled(toggle4Value == 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Tangani kesalahan jika perlu
            }
        });

        // Contoh untuk mengaktifkan animasi saat terjadi perubahan nilai suhu
        dataSuhu.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Memeriksa apakah nilai sebelumnya tidak sama dengan nilai sekarang
                if (!previousSuhuValue.equals(charSequence.toString())) {
                    // Menerapkan animasi fade in dan fade out pada TextView
                    updateTextViewWithAnimation(dataSuhu, charSequence.toString());
                }
                previousSuhuValue = charSequence.toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // Contoh untuk mengaktifkan animasi saat terjadi perubahan nilai kelembaban
        dataKelembaban.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Memeriksa apakah nilai sebelumnya tidak sama dengan nilai sekarang
                if (!previousKelembabanValue.equals(charSequence.toString())) {
                    // Menerapkan animasi fade in dan fade out pada TextView
                    updateTextViewWithAnimation(dataKelembaban, charSequence.toString());
                }
                previousKelembabanValue = charSequence.toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    // Metode untuk mengatur status toggle dan mendengarkan perubahan status dari Firebase
    private void setToggleStatus(DatabaseReference reference, ToggleButton toggle, KontrolStatus defaultStatus) {
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer value = snapshot.getValue(Integer.class);
                KontrolStatus status = (value != null && value == 1) ? KontrolStatus.ON : defaultStatus;
                toggle.setChecked(status == KontrolStatus.ON);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int kontrolValue = isChecked ? 1 : 0;
                reference.setValue(kontrolValue);
            }
        });
    }

    // Metode untuk menampilkan notifikasi
    private void showNotification(String message, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Buat saluran notifikasi (hanya diperlukan di Android Oreo ke atas)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("jamur_channel", "Jamur Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Gunakan NotificationCompat.Builder untuk dukungan versi Android yang lebih lama
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "jamur_channel")
                .setContentTitle("Notifikasi Jamur")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification);

        // Tampilkan notifikasi
        notificationManager.notify(notificationId, builder.build());
    }


    // Metode untuk menerapkan animasi fade in dan fade out pada TextView
    private void updateTextViewWithAnimation(TextView textView, String newText) {
        textView.animate()
                .alpha(0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        textView.setText(newText);
                        textView.animate()
                                .alpha(1f)
                                .setDuration(500)
                                .setListener(null);
                    }
                });
    }
}
