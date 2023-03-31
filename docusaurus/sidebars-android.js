module.exports = {
    mySidebar: [
        {
            type: 'category',
            label: 'Basics',
            items: [
                {
                    type: 'autogenerated',
                    dirName: '01-basics'
                }
            ]
        },
        {
            type: 'category',
            label: 'Migration Guides',
            items: [
                {
                    type: 'autogenerated',
                    dirName: '02-migration-guides'
                }
            ]
        },
        {
            type: 'category',
            label: 'Guides',
            items: [
                {
                    type: 'category',
                    label: 'Push Notifications',
                    items: [
                        {
                            type: 'autogenerated',
                            dirName: '03-client/06-guides/01-push-notifications'
                        }
                    ]
                },
                {
                    type: 'doc',
                    id: 'client/guides/handling-user-connection',
                },
                {
                    type: 'doc',
                    id: 'client/guides/offline-support',
                },
                {
                    type: 'doc',
                    id: 'client/guides/channel-list-updates',
                },
                {
                    type: 'doc',
                    id: 'client/guides/listening-for-events',
                },
                {
                    type: 'doc',
                    id: 'client/guides/sending-custom-attachments',
                },
                {
                    type: 'doc',
                    id: 'client/guides/optimizations',
                },
                {
                    type: 'category',
                    label: 'Video Integrations',
                    items: [
                        {
                            type: 'autogenerated',
                            dirName: '03-client/06-guides/10-video-integrations'
                        }
                    ]
                },
            ]
            // items: [
            //     {
            //         type: 'autogenerated',
            //         dirName: '03-client'
            //     }
            // ]
        },
        {
            type: 'category',
            label: 'UI Components',
            items: [
                {
                    type: 'autogenerated',
                    dirName: '04-ui'
                }
            ]
        },
        {
            type: 'category',
            label: 'Compose UI Components',
            items: [
                {
                    type: 'autogenerated',
                    dirName: '05-compose'
                }
            ]
        },
        {
            type: 'category',
            label: 'Additional Resources',
            items: [
                {
                    type: 'autogenerated',
                    dirName: '06-resources'
                }
            ]
        }
    ]
};
