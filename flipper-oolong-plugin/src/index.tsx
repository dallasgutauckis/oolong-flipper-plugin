import React from 'react';
import {
    PluginClient,
    usePlugin,
    createState,
    useValue,
    Layout,
    DetailSidebar,
    DataInspector
} from 'flipper-plugin';
import {Card, Divider, List, Row, Timeline, Typography} from 'antd';
import {TableRow} from "flipper-plugin/lib/ui/data-table/TableRow";

type Data = {
    id: number;
    type: string;
    data: string;
};

type Events = {
    newRow: Data;
};

// Read more: https://fbflipper.com/docs/tutorial/js-custom#creating-a-first-plugin
// API: https://fbflipper.com/docs/extending/flipper-plugin#pluginclient
export function plugin(client: PluginClient<Events, {}>) {
    const data = createState<Record<string, Data>>({}, {persist: 'data'});
    const selectedID = createState<string | null>(null, {persist: 'selection'});

    client.onMessage('newRow', (newData) => {
        data.update((draft) => {
            draft[newData.id] = newData;
        });
    });

    client.addMenuEntry({
        action: 'clear',
        handler: async () => {
            data.set({});
        },
        accelerator: 'ctrl+l',
    });

    function setSelection(id: string) {
        selectedID.set(id);
    }

    return {data, selectedID, setSelection};
}

// Read more: https://fbflipper.com/docs/tutorial/js-custom#building-a-user-interface-for-the-plugin
// API: https://fbflipper.com/docs/extending/flipper-plugin#react-hooks
export function Component() {
    const instance = usePlugin(plugin);
    const data = useValue(instance.data);
    const selectedID = useValue(instance.selectedID);
    const rows = data;

    return (
        <>
            <Layout.ScrollContainer pad>
                <List
                    dataSource={Object.entries(data)}
                    renderItem={([id, d]) => {
                        let onSelect = () => {
                            instance.setSelection(id)
                        }

                        return (
                            <FunctionCallRow
                                type={d.type}
                                data={JSON.stringify(d.data)}
                                selected={id === selectedID}
                                onSelect={onSelect}
                            />
                        )
                    }
                    }
                />
            </Layout.ScrollContainer>
            <DetailSidebar>
                {selectedID && renderSidebar(rows[selectedID])}
            </DetailSidebar>
        </>
    );
}

type FunctionCallRowProps = {
    type: string
    data: String
    selected: boolean
    onSelect: () => void
}

function FunctionCallRow(props: FunctionCallRowProps) {
    return (
        <div onClick={props.onSelect}>
            <Typography.Text>{props.type}</Typography.Text>
            <Typography.Text>{" // "}</Typography.Text>
            <Typography.Text>{props.data}</Typography.Text>
        </div>
    )
}

function renderSidebar(data: Data) {
    return (
        <Layout.Container gap pad>
            <Typography.Title level={4}>{data.type}</Typography.Title>
            <DataInspector data={data.data} expandRoot={true}/>
        </Layout.Container>
    );
}
