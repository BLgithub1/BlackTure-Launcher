package net.kdt.pojavlaunch.fragments;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.obvilion.launcher.Vars;
import ru.obvilion.launcher.tasks.ImageLoadTask;
import ru.obvilion.launcher.utils.JsonUtils;

public class ServerFragment extends Fragment {
    private JSONObject data = null;

    public ServerFragment(JSONObject obj) {
        data = obj;
    }

    public ServerFragment() {
        try {
            if (Vars.SERVERS == null) {
                Vars.SERVERS = JsonUtils.readJsonFromFile(Vars.SERVERS_JSON).getJSONArray("servers");
                Map<Integer, String> _do = new HashMap<>();

                JSONArray temp = new JSONArray();

                for (int i = 0; i < Vars.SERVERS.length(); i++) {
                    JSONObject tec = Vars.SERVERS.getJSONObject(i);
                    _do.put(i, tec.getString("name"));
                }

                List<Map.Entry<Integer, String>> entries = new ArrayList<>(_do.entrySet());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    entries.sort(Map.Entry.comparingByValue());
                }

                for (Map.Entry<Integer, String> entry : entries) {
                    temp.put(Vars.SERVERS.getJSONObject(entry.getKey()));
                }

                Vars.SERVERS = temp;
            }

            if (Vars.SERVERS.length() <= Vars.SERVERS_TMP) {
                Vars.SERVERS_TMP = 0;
            }

            data = Vars.SERVERS.getJSONObject(Vars.SERVERS_TMP);

            Vars.SERVERS_TMP++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.servers_tab, container, false);

        TextView desc = view.findViewById(R.id.server_description);
        TextView name = view.findViewById(R.id.server_name);

        try {
            desc.setText(data.getString("description"));
            name.setText(data.getString("name") + " - " + data.getString("version"));
        } catch (Exception e) {
            Log.e("Server", e.getLocalizedMessage());
        }

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    public void setBG() {
        try {
            Vars.LAUNCHER_ACTIVITY.editBG(data.getString("image"));
        } catch (Exception e) {
            Log.e("Server", e.getLocalizedMessage());
        }
    }
}
