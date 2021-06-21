package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

import org.json.JSONObject;

import ru.obvilion.launcher.Vars;
import ru.obvilion.launcher.tasks.ImageLoadTask;

public class ServerFragment extends Fragment {
    private JSONObject data;

    public ServerFragment(JSONObject obj) {
        data = obj;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.servers_tab, container, false);

        TextView desc = view.findViewById(R.id.server_description);
        TextView name = view.findViewById(R.id.server_name);

        try {
            desc.setText(data.getString("description"));
            name.setText(data.getString("name"));
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
